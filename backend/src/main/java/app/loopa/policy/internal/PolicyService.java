package app.loopa.policy.internal;

import app.loopa.catalog.OrderView;
import app.loopa.policy.Eligibility;
import app.loopa.policy.PolicyApi;
import app.loopa.policy.PolicyView;
import app.loopa.policy.ReasonView;
import app.loopa.shared.tenant.TenantContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {

    Optional<PolicyEntity> findFirstBy();
}

interface ReasonRepository extends JpaRepository<ReasonEntity, UUID> {

    List<ReasonEntity> findAllByOrderByPosition();

    List<ReasonEntity> findByActiveTrueOrderByPosition();
}

@Service
class PolicyService implements PolicyApi {

    private final PolicyRepository policies;
    private final ReasonRepository reasons;
    private final Clock clock;

    PolicyService(PolicyRepository policies, ReasonRepository reasons, Clock clock) {
        this.policies = policies;
        this.reasons = reasons;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PolicyView policy() {
        return toView(getOrCreate());
    }

    @Override
    @Transactional
    public List<ReasonView> activeReasons() {
        ensureDefaultReasons();
        return reasons.findByActiveTrueOrderByPosition().stream().map(PolicyService::toView).toList();
    }

    @Override
    @Transactional
    public Eligibility evaluate(OrderView order, Map<String, Integer> alreadyReturned) {
        PolicyView p = toView(getOrCreate());
        Instant reference = order.fulfilledAt() != null ? order.fulfilledAt() : order.orderedAt();
        Instant until = reference.plus(Duration.ofDays(p.windowDays()));
        boolean windowOpen = clock.instant().isBefore(until);
        List<Eligibility.LineEligibility> lines = new ArrayList<>();
        for (OrderView.Line line : order.lines()) {
            int used = alreadyReturned.getOrDefault(line.externalLineId(), 0);
            int remaining = Math.max(0, line.quantity() - used);
            String reason = null;
            if (!windowOpen) {
                reason = "WINDOW_EXPIRED";
            } else if (line.productType() != null && p.excludedProductTypes().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(line.productType()))) {
                reason = "EXCLUDED_PRODUCT";
            } else if (remaining == 0) {
                reason = "ALREADY_RETURNED";
            }
            lines.add(new Eligibility.LineEligibility(line.externalLineId(), reason == null ? remaining : 0,
                    reason == null, reason));
        }
        return new Eligibility(until, windowOpen, lines);
    }

    @Override
    @Transactional
    public boolean shouldAutoApprove(long totalMinor, List<String> reasonCodes) {
        PolicyView p = toView(getOrCreate());
        if (!p.autoApprove() || totalMinor > p.autoApproveMaxAmountMinor()) {
            return false;
        }
        List<ReasonView> all = activeReasons();
        return reasonCodes.stream().noneMatch(code -> all.stream()
                .anyMatch(r -> r.code().equals(code) && r.requiresPhoto()));
    }

    /* ---------- utilisés par le contrôleur admin ---------- */

    @Transactional
    PolicyView update(PolicyView in) {
        PolicyEntity e = getOrCreate();
        e.windowDays = in.windowDays();
        e.autoApprove = in.autoApprove();
        e.autoApproveMaxAmountMinor = in.autoApproveMaxAmountMinor();
        e.allowRefund = in.allowRefund();
        e.allowExchange = in.allowExchange();
        e.allowStoreCredit = in.allowStoreCredit();
        e.storeCreditBonusMinor = in.storeCreditBonusMinor();
        e.excludedProductTypes = String.join(",", in.excludedProductTypes());
        return toView(e);
    }

    @Transactional
    List<ReasonView> allReasons() {
        ensureDefaultReasons();
        return reasons.findAllByOrderByPosition().stream().map(PolicyService::toView).toList();
    }

    @Transactional
    List<ReasonView> replaceReasons(List<ReasonView> incoming) {
        List<ReasonEntity> existing = reasons.findAllByOrderByPosition();
        int position = 0;
        for (ReasonView r : incoming) {
            ReasonEntity e = existing.stream().filter(x -> x.code.equals(r.code())).findFirst().orElseGet(() -> {
                ReasonEntity n = new ReasonEntity();
                n.id = UUID.randomUUID();
                n.tenantId = TenantContext.require();
                n.code = r.code();
                return n;
            });
            e.labelFr = r.labelFr();
            e.labelEn = r.labelEn();
            e.labelAr = r.labelAr();
            e.requiresPhoto = r.requiresPhoto();
            e.active = r.active();
            e.position = position++;
            reasons.save(e);
        }
        // Un motif retiré de la liste est désactivé, jamais supprimé : il reste lisible sur les anciens retours.
        existing.stream()
                .filter(e -> incoming.stream().noneMatch(r -> r.code().equals(e.code)))
                .forEach(e -> e.active = false);
        return allReasons();
    }

    private PolicyEntity getOrCreate() {
        return policies.findFirstBy().orElseGet(() -> {
            PolicyEntity e = new PolicyEntity();
            e.id = UUID.randomUUID();
            e.tenantId = TenantContext.require();
            e.windowDays = 30;
            e.autoApprove = true;
            e.autoApproveMaxAmountMinor = 100_000;
            e.allowRefund = true;
            e.allowExchange = true;
            e.allowStoreCredit = true;
            e.storeCreditBonusMinor = 5_000;
            e.excludedProductTypes = "";
            return policies.save(e);
        });
    }

    private void ensureDefaultReasons() {
        if (reasons.count() > 0) {
            return;
        }
        UUID tenant = TenantContext.require();
        String[][] defaults = {
                {"TOO_SMALL", "Trop petit", "Too small", "صغير جدًا", "false"},
                {"TOO_BIG", "Trop grand", "Too big", "كبير جدًا", "false"},
                {"DAMAGED", "Article endommagé", "Damaged item", "منتج تالف", "true"},
                {"NOT_AS_DESCRIBED", "Différent de la description", "Not as described", "مختلف عن الوصف", "true"},
                {"CHANGED_MIND", "J'ai changé d'avis", "Changed my mind", "غيرت رأيي", "false"}
        };
        for (int i = 0; i < defaults.length; i++) {
            ReasonEntity e = new ReasonEntity();
            e.id = UUID.randomUUID();
            e.tenantId = tenant;
            e.code = defaults[i][0];
            e.labelFr = defaults[i][1];
            e.labelEn = defaults[i][2];
            e.labelAr = defaults[i][3];
            e.requiresPhoto = Boolean.parseBoolean(defaults[i][4]);
            e.active = true;
            e.position = i;
            reasons.save(e);
        }
    }

    private static PolicyView toView(PolicyEntity e) {
        List<String> excluded = e.excludedProductTypes == null || e.excludedProductTypes.isBlank()
                ? List.of()
                : Arrays.stream(e.excludedProductTypes.split(",")).map(String::trim)
                        .filter(s -> !s.isEmpty()).toList();
        return new PolicyView(e.windowDays, e.autoApprove, e.autoApproveMaxAmountMinor, e.allowRefund,
                e.allowExchange, e.allowStoreCredit, e.storeCreditBonusMinor, excluded);
    }

    private static ReasonView toView(ReasonEntity e) {
        return new ReasonView(e.code, e.labelFr, e.labelEn, e.labelAr, e.requiresPhoto, e.active, e.position);
    }

    static boolean sameCode(ReasonView a, ReasonView b) {
        return Objects.equals(a.code(), b.code());
    }
}
