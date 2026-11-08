package app.loopa.returns.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.loopa.returns.ReturnEvents.ReturnApproved;
import app.loopa.returns.ReturnEvents.ReturnSubmitted;
import app.loopa.shared.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReturnRequestTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T10:00:00Z"), ZoneOffset.UTC);

    private ReturnRequest newReturn(ResolutionType type) {
        var lines = List.of(
                new ReturnLine(null, "line-1", "Chemise lin", "M", 2, Money.of(29900, "MAD"), "TOO_SMALL", null),
                new ReturnLine(null, "line-2", "Pantalon", "40", 1, Money.of(45000, "MAD"), "DAMAGED", null));
        var data = new ReturnRequest.NewReturnData(UUID.randomUUID(), UUID.randomUUID(), "#1042", "Nadia B.",
                "client@example.com", "fr", type, lines, List.of(), null, 5000);
        return ReturnRequest.submit(data, "RMA-2026-000001", clock);
    }

    @Test
    void submitCreatesSubmittedReturnAndEvent() {
        var r = newReturn(ResolutionType.STORE_CREDIT);

        assertThat(r.status()).isEqualTo(ReturnStatus.SUBMITTED);
        assertThat(r.total()).isEqualTo(Money.of(104800, "MAD"));
        assertThat(r.creditAmount()).isEqualTo(Money.of(109800, "MAD"));
        assertThat(r.pullEvents()).singleElement().isInstanceOf(ReturnSubmitted.class);
    }

    @Test
    void bonusOnlyAppliesToStoreCredit() {
        var r = newReturn(ResolutionType.REFUND);

        assertThat(r.bonusMinor()).isZero();
        assertThat(r.creditAmount()).isEqualTo(r.total());
    }

    @Test
    void happyPathGoesToResolved() {
        var r = newReturn(ResolutionType.STORE_CREDIT);
        r.approve(true, clock);
        r.ship("Amana", "AM123456789MA", clock);
        r.receive(clock);
        r.resolve("gid://shopify/GiftCard/1", clock);

        assertThat(r.status()).isEqualTo(ReturnStatus.RESOLVED);
        assertThat(r.status().isFinal()).isTrue();
        assertThat(r.approvedAt()).isNotNull();
        var events = r.pullEvents();
        assertThat(events).hasSize(5);
        assertThat(events.get(1)).isInstanceOf(ReturnApproved.class);
    }

    @Test
    void cannotResolveBeforeReception() {
        var r = newReturn(ResolutionType.REFUND);
        r.approve(false, clock);

        assertThatThrownBy(() -> r.resolve("x", clock))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("APPROVED").hasMessageContaining("RESOLVED");
    }

    @Test
    void failedResolutionKeepsReturnReceived() {
        var r = newReturn(ResolutionType.REFUND);
        r.approve(false, clock);
        r.receive(clock);
        r.resolutionFailed("Shopify indisponible", clock);

        assertThat(r.status()).isEqualTo(ReturnStatus.RECEIVED);
        assertThat(r.resolutionError()).isEqualTo("Shopify indisponible");
    }

    @Test
    void rejectionNeedsAReason() {
        var r = newReturn(ResolutionType.REFUND);

        assertThatThrownBy(() -> r.reject(" ", clock)).isInstanceOf(IllegalArgumentException.class);
        assertThat(r.status()).isEqualTo(ReturnStatus.SUBMITTED);
    }

    @Test
    void finalStatesAllowNoTransition() {
        assertThat(ReturnStatus.REJECTED.allowedNext()).isEmpty();
        assertThat(ReturnStatus.CANCELLED.allowedNext()).isEmpty();
        assertThat(ReturnStatus.RESOLVED.allowedNext()).isEmpty();
    }
}
