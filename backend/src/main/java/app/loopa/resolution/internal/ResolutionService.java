package app.loopa.resolution.internal;

import app.loopa.integration.IntegrationApi;
import app.loopa.returns.ReturnEvents.ResolutionRetryRequested;
import app.loopa.returns.ReturnEvents.ReturnReceived;
import app.loopa.returns.ReturnView;
import app.loopa.returns.ReturnsApi;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Exécute la résolution dès qu'un retour est reçu, ou à la demande du marchand après un échec.
 * Port de sortie implicite : Shopify pour les boutiques Shopify, simulation pour la boutique de démo.
 */
@Service
class ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private final ReturnsApi returns;
    private final TenancyApi tenancy;
    private final IntegrationApi integration;

    ResolutionService(ReturnsApi returns, TenancyApi tenancy, IntegrationApi integration) {
        this.returns = returns;
        this.tenancy = tenancy;
        this.integration = integration;
    }

    @Async
    @TransactionalEventListener
    public void on(ReturnReceived event) {
        TenantContext.runAs(event.tenantId(), () -> execute(event.tenantId(), event.returnId()));
    }

    @Async
    @TransactionalEventListener
    public void on(ResolutionRetryRequested event) {
        TenantContext.runAs(event.tenantId(), () -> execute(event.tenantId(), event.returnId()));
    }

    void execute(UUID shopId, UUID returnId) {
        ReturnView r = returns.find(returnId).orElse(null);
        ShopView shop = tenancy.findById(shopId).orElse(null);
        if (r == null || shop == null || !"RECEIVED".equals(r.status())) {
            return;
        }
        try {
            String ref = switch (shop.platform()) {
                case "SHOPIFY" -> integration.executeShopifyResolution(shopId, r);
                default -> simulated(r);
            };
            returns.markResolved(returnId, ref);
            log.info("Retour {} résolu ({}) : {}", r.number(), r.resolutionType(), ref);
        } catch (RuntimeException ex) {
            log.warn("Résolution du retour {} en échec : {}", r.number(), ex.getMessage());
            returns.markResolutionFailed(returnId, ex.getMessage());
        }
    }

    /** Boutique de démonstration : aucune plateforme externe, la résolution est enregistrée telle quelle. */
    private static String simulated(ReturnView r) {
        String prefix = switch (r.resolutionType()) {
            case "REFUND" -> "REFUND";
            case "EXCHANGE" -> "EXCHANGE";
            default -> "GIFTCARD";
        };
        return prefix + "-" + r.number().substring(r.number().lastIndexOf('-') + 1);
    }
}
