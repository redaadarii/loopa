package app.loopa.billing.internal;

import app.loopa.security.Principals;
import app.loopa.shared.BusinessException;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abonnement. Pour une boutique Shopify, le changement de plan payant passe par la Billing API de Shopify
 * (le marchand valide le paiement sur Shopify) ; pour la démo, le changement est immédiat.
 */
@RestController
@RequestMapping("/api/admin/v1/billing")
@Tag(name = "Admin - Abonnement")
class BillingController {

    private final BillingService billing;
    private final TenancyApi tenancy;

    BillingController(BillingService billing, TenancyApi tenancy) {
        this.billing = billing;
        this.tenancy = tenancy;
    }

    record SubscribeRequest(@NotBlank String plan) {
    }

    record SubscribeResponse(String plan, String confirmationUrl) {
    }

    @GetMapping
    BillingService.Usage usage() {
        return billing.usage(Principals.currentShopId());
    }

    @PostMapping("/subscribe")
    SubscribeResponse subscribe(@Valid @RequestBody SubscribeRequest body) {
        BillingService.Plan plan = BillingService.PLANS.stream().filter(p -> p.code().equals(body.plan()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("UNKNOWN_PLAN", HttpStatus.UNPROCESSABLE_ENTITY,
                        "Plan inconnu : " + body.plan()));
        ShopView shop = tenancy.findById(Principals.currentShopId()).orElseThrow();
        if ("SHOPIFY".equals(shop.platform()) && plan.priceMinor() > 0) {
            // TODO : appSubscriptionCreate (GraphQL) puis redirection vers confirmationUrl ;
            // le plan est activé à la réception du webhook app_subscriptions/update.
            throw new BusinessException("SHOPIFY_BILLING_PENDING", HttpStatus.NOT_IMPLEMENTED,
                    "La facturation via Shopify sera activée lors de la publication sur l'App Store.");
        }
        tenancy.changePlan(shop.id(), plan.code());
        return new SubscribeResponse(plan.code(), null);
    }
}
