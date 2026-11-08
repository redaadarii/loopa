package app.loopa.analytics.internal;

import app.loopa.security.Principals;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Indicateurs du tableau de bord, calculés directement en SQL (lecture seule, filtrés par boutique).
 * Si le volume grandit : vues matérialisées rafraîchies par les événements du module returns.
 */
@RestController
@RequestMapping("/api/admin/v1/analytics")
@Tag(name = "Admin - Statistiques")
class AnalyticsController {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    AnalyticsController(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    record Count(String key, long count) {
    }

    record Day(LocalDate date, long count) {
    }

    record Summary(int days, String currency, long totalReturns, long pending, long approved, long rejected,
                   long resolved, long retainedMinor, long refundedMinor, long bonusMinor,
                   Double medianApprovalMinutes, long autoApproved, List<Count> reasons, List<Count> resolutions,
                   List<Count> topProducts, List<Day> daily) {
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    Summary summary(@RequestParam(defaultValue = "30") int days) {
        int period = Math.clamp(days, 1, 365);
        UUID shop = Principals.currentShopId();
        Instant since = clock.instant().truncatedTo(ChronoUnit.DAYS).minus(period - 1L, ChronoUnit.DAYS);
        Timestamp from = Timestamp.from(since);

        Map<String, Long> byStatus = new HashMap<>();
        jdbc.query("select status, count(*) from return_request where tenant_id = ? and created_at >= ? group by status",
                rs -> { byStatus.put(rs.getString(1), rs.getLong(2)); }, shop, from);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        // Valeur conservée = avoirs et échanges (hors retours rejetés/annulés) ; remboursée = remboursements.
        Map<String, Long> amounts = new HashMap<>();
        jdbc.query("""
                select r.resolution_type, coalesce(sum(l.quantity * l.unit_price_minor), 0), coalesce(max(l.currency), '')
                from return_request r join return_line l on l.return_request_id = r.id
                where r.tenant_id = ? and r.created_at >= ? and r.status not in ('REJECTED', 'CANCELLED')
                group by r.resolution_type
                """, rs -> { amounts.put(rs.getString(1), rs.getLong(2)); }, shop, from);
        String currency = jdbc.query("select currency from shop where id = ?",
                rs -> rs.next() ? rs.getString(1) : "MAD", shop);

        Long bonus = jdbc.queryForObject("""
                select coalesce(sum(bonus_minor), 0) from return_request
                where tenant_id = ? and created_at >= ? and status not in ('REJECTED', 'CANCELLED')
                """, Long.class, shop, from);

        Double median = jdbc.queryForObject("""
                select percentile_cont(0.5) within group (order by extract(epoch from (approved_at - created_at)) / 60)
                from return_request where tenant_id = ? and created_at >= ? and approved_at is not null
                """, Double.class, shop, from);

        Long auto = jdbc.queryForObject("""
                select count(*) from return_event e join return_request r on r.id = e.return_request_id
                where r.tenant_id = ? and r.created_at >= ? and e.type = 'APPROVED' and e.actor = 'SYSTEM'
                """, Long.class, shop, from);

        List<Count> reasons = jdbc.query("""
                select l.reason_code, count(*) from return_line l join return_request r on r.id = l.return_request_id
                where r.tenant_id = ? and r.created_at >= ? group by l.reason_code order by count(*) desc
                """, (rs, i) -> new Count(rs.getString(1), rs.getLong(2)), shop, from);

        List<Count> resolutions = jdbc.query("""
                select resolution_type, count(*) from return_request
                where tenant_id = ? and created_at >= ? and status not in ('REJECTED', 'CANCELLED')
                group by resolution_type order by count(*) desc
                """, (rs, i) -> new Count(rs.getString(1), rs.getLong(2)), shop, from);

        List<Count> products = jdbc.query("""
                select l.title, sum(l.quantity) from return_line l join return_request r on r.id = l.return_request_id
                where r.tenant_id = ? and r.created_at >= ? group by l.title order by sum(l.quantity) desc limit 5
                """, (rs, i) -> new Count(rs.getString(1), rs.getLong(2)), shop, from);

        Map<LocalDate, Long> perDay = new LinkedHashMap<>();
        for (int i = 0; i < period; i++) {
            perDay.put(LocalDate.ofInstant(since, ZoneOffset.UTC).plusDays(i), 0L);
        }
        jdbc.query("""
                select cast(created_at at time zone 'UTC' as date), count(*) from return_request
                where tenant_id = ? and created_at >= ? group by 1
                """, rs -> { perDay.put(rs.getObject(1, LocalDate.class), rs.getLong(2)); }, shop, from);
        List<Day> daily = new ArrayList<>();
        perDay.forEach((d, c) -> daily.add(new Day(d, c)));

        long retained = amounts.getOrDefault("STORE_CREDIT", 0L) + amounts.getOrDefault("EXCHANGE", 0L);
        return new Summary(period, currency, total,
                byStatus.getOrDefault("SUBMITTED", 0L),
                byStatus.getOrDefault("APPROVED", 0L) + byStatus.getOrDefault("IN_TRANSIT", 0L)
                        + byStatus.getOrDefault("RECEIVED", 0L),
                byStatus.getOrDefault("REJECTED", 0L),
                byStatus.getOrDefault("RESOLVED", 0L),
                retained, amounts.getOrDefault("REFUND", 0L), bonus == null ? 0 : bonus,
                median, auto == null ? 0 : auto, reasons, resolutions, products, daily);
    }
}
