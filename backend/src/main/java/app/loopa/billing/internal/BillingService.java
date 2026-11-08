package app.loopa.billing.internal;

import app.loopa.billing.BillingApi;
import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Plans et quotas. L'usage est calculé à la volée (retours créés depuis le 1er du mois),
 * sans compteur à maintenir.
 */
@Service
class BillingService implements BillingApi {

    /** Catalogue des plans. Prix en unités mineures (centimes de MAD) ; quota -1 = illimité. */
    static final List<Plan> PLANS = List.of(
            new Plan("STARTER", "Starter", 0, "MAD", 20,
                    List.of("Portail de retour", "Remboursements", "1 utilisateur")),
            new Plan("GROWTH", "Growth", 29_000, "MAD", 300,
                    List.of("Échanges et avoirs", "Bonus shop-now", "Statistiques", "5 utilisateurs")),
            new Plan("SCALE", "Scale", 89_000, "MAD", -1,
                    List.of("Retours illimités", "Transporteurs", "API et webhooks", "Utilisateurs illimités")));

    record Plan(String code, String name, long priceMinor, String currency, int quota, List<String> features) {
    }

    record Usage(String plan, int quota, long used, LocalDate periodStart, List<Plan> plans) {
    }

    private final TenancyApi tenancy;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    BillingService(TenancyApi tenancy, JdbcTemplate jdbc, Clock clock) {
        this.tenancy = tenancy;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void assertCanCreateReturn() {
        Usage usage = usage(TenantContext.require());
        if (usage.quota() >= 0 && usage.used() >= usage.quota()) {
            throw new BusinessException("QUOTA_REACHED", HttpStatus.PAYMENT_REQUIRED,
                    "La boutique a atteint son quota de retours ce mois-ci. Contactez-la directement.");
        }
    }

    Usage usage(UUID shopId) {
        ShopView shop = tenancy.findById(shopId).orElseThrow(() -> BusinessException.notFound("Boutique", shopId));
        Plan plan = plan(shop.plan());
        LocalDate periodStart = LocalDate.now(clock).withDayOfMonth(1);
        Long used = jdbc.queryForObject(
                "select count(*) from return_request where tenant_id = ? and created_at >= ?",
                Long.class, shopId, periodStart.atStartOfDay().toInstant(ZoneOffset.UTC).atOffset(ZoneOffset.UTC));
        return new Usage(plan.code(), plan.quota(), used == null ? 0 : used, periodStart, PLANS);
    }

    static Plan plan(String code) {
        return PLANS.stream().filter(p -> p.code().equals(code)).findFirst().orElse(PLANS.getFirst());
    }
}
