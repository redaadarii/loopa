package app.loopa.portal.internal;

import app.loopa.policy.PolicyApi;
import app.loopa.returns.ReturnView;
import app.loopa.security.Principals;
import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.storage.StorageApi;
import app.loopa.storage.StoredFile;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API du portail client.
 * <ul>
 *   <li>Publiques : {@code /shops/{handle}}, {@code /sessions}, {@code /sessions/verify} ;</li>
 *   <li>Avec jeton portail (après code) : {@code /order}, {@code /attachments}, {@code /returns/**}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/portal/v1")
@Tag(name = "Portail client")
class PortalController {

    private final TenancyApi tenancy;
    private final PolicyApi policy;
    private final PortalSessionService sessions;
    private final PortalReturnService portalReturns;
    private final StorageApi storage;

    PortalController(TenancyApi tenancy, PolicyApi policy, PortalSessionService sessions,
                     PortalReturnService portalReturns, StorageApi storage) {
        this.tenancy = tenancy;
        this.policy = policy;
        this.sessions = sessions;
        this.portalReturns = portalReturns;
        this.storage = storage;
    }

    /* ---------- public ---------- */

    record ShopDto(String handle, String name, String primaryColor, String logoUrl, String welcomeMessage,
                   String currency, String defaultLocale, int windowDays, long storeCreditBonusMinor) {
    }

    record StartSessionRequest(@NotBlank @Size(max = 64) String shop, @NotBlank @Size(max = 32) String orderNumber,
                               @NotBlank @Size(max = 254) String contact,
                               @Pattern(regexp = "fr|en|ar") String locale) {
    }

    record VerifyRequest(@NotNull UUID sessionId, @NotBlank @Pattern(regexp = "\\d{6}") String code) {
    }

    @GetMapping("/shops/{handle}")
    ShopDto shop(@PathVariable String handle) {
        ShopView shop = activeShop(handle);
        return TenantContext.callAs(shop.id(), () -> {
            var p = policy.policy();
            return new ShopDto(shop.handle(), shop.name(), shop.primaryColor(), shop.logoUrl(), shop.welcomeMessage(),
                    shop.currency(), shop.defaultLocale(), p.windowDays(), p.storeCreditBonusMinor());
        });
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    PortalSessionService.SessionStarted startSession(@Valid @RequestBody StartSessionRequest body) {
        ShopView shop = activeShop(body.shop());
        return TenantContext.callAs(shop.id(),
                () -> sessions.start(shop, body.orderNumber(), body.contact(), body.locale()));
    }

    @PostMapping("/sessions/verify")
    PortalSessionService.SessionVerified verify(@Valid @RequestBody VerifyRequest body) {
        return sessions.verify(body.sessionId(), body.code());
    }

    /* ---------- avec jeton portail ---------- */

    record TrackingRequest(@Size(max = 40) String carrier, @NotBlank @Size(max = 64) String trackingNumber) {
    }

    record AttachmentDto(UUID id, String filename, String contentType, long size, Instant createdAt) {

        static AttachmentDto from(StoredFile f) {
            return new AttachmentDto(f.id(), f.filename(), f.contentType(), f.size(), f.createdAt());
        }
    }

    /** Vue client d'un retour : sans les noms des membres de l'équipe ni les notes internes. */
    record CustomerReturnDto(UUID id, String number, String status, String resolutionType, long totalMinor,
                             long bonusMinor, long creditMinor, String currency, String rejectionReason,
                             String carrier, String trackingNumber, List<ReturnView.Line> lines,
                             List<Step> steps, Instant createdAt) {

        record Step(String type, Instant occurredAt) {
        }

        static CustomerReturnDto from(ReturnView r) {
            List<Step> steps = r.history().stream()
                    .filter(h -> !"NOTE".equals(h.type()) && !"RESOLUTION_RETRY".equals(h.type())
                            && !"RESOLUTION_FAILED".equals(h.type()))
                    .map(h -> new Step(h.type(), h.occurredAt())).toList();
            return new CustomerReturnDto(r.id(), r.number(), r.status(), r.resolutionType(), r.totalMinor(),
                    r.bonusMinor(), r.creditMinor(), r.currency(), r.rejectionReason(), r.carrier(),
                    r.trackingNumber(), r.lines(), steps, r.createdAt());
        }
    }

    @GetMapping("/order")
    PortalReturnService.OrderDto order() {
        return portalReturns.order(Principals.portalOrderId());
    }

    @PostMapping(path = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    AttachmentDto upload(@RequestPart("file") MultipartFile file) {
        try {
            return AttachmentDto.from(storage.store(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    file.getInputStream()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostMapping("/returns")
    ResponseEntity<CustomerReturnDto> create(@RequestBody PortalReturnService.SubmitRequest body) {
        ReturnView created = portalReturns.submit(Principals.portalOrderId(), body);
        return ResponseEntity.created(URI.create("/api/portal/v1/returns/" + created.id()))
                .body(CustomerReturnDto.from(portalReturns.get(Principals.portalOrderId(), created.id())));
    }

    @GetMapping("/returns/{id}")
    CustomerReturnDto get(@PathVariable UUID id) {
        return CustomerReturnDto.from(portalReturns.get(Principals.portalOrderId(), id));
    }

    @PostMapping("/returns/{id}/tracking")
    CustomerReturnDto tracking(@PathVariable UUID id, @Valid @RequestBody TrackingRequest body) {
        portalReturns.addTracking(Principals.portalOrderId(), id, body.carrier(), body.trackingNumber());
        return get(id);
    }

    @PostMapping("/returns/{id}/cancel")
    CustomerReturnDto cancel(@PathVariable UUID id) {
        portalReturns.cancel(Principals.portalOrderId(), id);
        return get(id);
    }

    private ShopView activeShop(String handle) {
        return tenancy.findByHandle(handle).filter(ShopView::active)
                .orElseThrow(() -> BusinessException.notFound("Boutique", handle));
    }
}
