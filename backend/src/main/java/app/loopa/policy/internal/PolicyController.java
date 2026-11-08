package app.loopa.policy.internal;

import app.loopa.policy.PolicyView;
import app.loopa.policy.ReasonView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Réglages de la politique de retour et des motifs. Lecture : toute l'équipe ; écriture : admins. */
@RestController
@RequestMapping("/api/admin/v1/settings")
@Tag(name = "Admin - Politique de retour")
class PolicyController {

    private final PolicyService service;

    PolicyController(PolicyService service) {
        this.service = service;
    }

    record PolicyDto(
            @Min(1) @Max(365) int windowDays,
            boolean autoApprove,
            @PositiveOrZero long autoApproveMaxAmountMinor,
            boolean allowRefund,
            boolean allowExchange,
            boolean allowStoreCredit,
            @PositiveOrZero long storeCreditBonusMinor,
            @NotNull List<@NotBlank String> excludedProductTypes) {
    }

    record ReasonDto(
            @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,40}") String code,
            @NotBlank @Size(max = 80) String labelFr,
            @NotBlank @Size(max = 80) String labelEn,
            @NotBlank @Size(max = 80) String labelAr,
            boolean requiresPhoto,
            boolean active) {
    }

    @GetMapping("/policy")
    PolicyView policy() {
        return service.policy();
    }

    @PutMapping("/policy")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    PolicyView updatePolicy(@Valid @RequestBody PolicyDto body) {
        if (!body.allowRefund() && !body.allowExchange() && !body.allowStoreCredit()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Au moins une solution (remboursement, échange ou avoir) doit rester proposée.");
        }
        return service.update(new PolicyView(body.windowDays(), body.autoApprove(), body.autoApproveMaxAmountMinor(),
                body.allowRefund(), body.allowExchange(), body.allowStoreCredit(), body.storeCreditBonusMinor(),
                body.excludedProductTypes()));
    }

    @GetMapping("/reasons")
    List<ReasonView> reasons() {
        return service.allReasons();
    }

    @PutMapping("/reasons")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    List<ReasonView> replaceReasons(@Valid @RequestBody @NotEmpty List<@Valid ReasonDto> body) {
        return service.replaceReasons(body.stream()
                .map(r -> new ReasonView(r.code(), r.labelFr(), r.labelEn(), r.labelAr(), r.requiresPhoto(),
                        r.active(), 0))
                .toList());
    }
}
