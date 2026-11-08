package app.loopa.returns.adapter.in.web;

import app.loopa.returns.ReturnView;
import app.loopa.returns.application.ReturnService;
import app.loopa.returns.domain.ReturnStatus;
import app.loopa.security.Principals;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Back-office marchand. Les changements d'état sont des commandes explicites (POST /approve, /reject…),
 * jamais un PATCH libre du statut.
 */
@RestController
@RequestMapping("/api/admin/v1/returns")
@Tag(name = "Admin - Retours")
class ReturnAdminController {

    private final ReturnService service;

    ReturnAdminController(ReturnService service) {
        this.service = service;
    }

    record ReturnPage(List<ReturnView> items, UUID nextCursor, Map<ReturnStatus, Long> counts) {
    }

    record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    record NoteRequest(@NotBlank @Size(max = 1000) String note) {
    }

    @GetMapping
    ReturnPage list(@RequestParam(required = false) ReturnStatus status,
                    @RequestParam(required = false) String q,
                    @RequestParam(required = false) UUID cursor,
                    @RequestParam(defaultValue = "25") int limit) {
        int size = Math.clamp(limit, 1, 100);
        List<ReturnView> items = service.search(status, q, cursor, size);
        UUID next = items.size() == size ? items.getLast().id() : null;
        return new ReturnPage(items, next, service.countByStatus());
    }

    record BulkApproveRequest(@jakarta.validation.constraints.NotEmpty
                              @jakarta.validation.constraints.Size(max = 100) List<UUID> ids) {
    }

    record BulkResult(List<UUID> approved, Map<UUID, String> failed) {
    }

    /** Approbation groupée : chaque retour est traité dans sa propre transaction, un échec n'annule pas les autres. */
    @PostMapping("/bulk-approve")
    BulkResult bulkApprove(@Valid @RequestBody BulkApproveRequest body) {
        List<UUID> approved = new java.util.ArrayList<>();
        Map<UUID, String> failed = new java.util.LinkedHashMap<>();
        String actor = Principals.currentActorName();
        for (UUID id : body.ids()) {
            try {
                service.approve(id, actor);
                approved.add(id);
            } catch (RuntimeException e) {
                failed.put(id, e.getMessage());
            }
        }
        return new BulkResult(approved, failed);
    }

    /** Export CSV (séparateur « ; », UTF-8 avec BOM : s'ouvre directement dans Excel). */
    @GetMapping(value = "/export.csv", produces = "text/csv")
    org.springframework.http.ResponseEntity<String> export(@RequestParam(required = false) ReturnStatus status) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Numéro;Date;Commande;Client;Email;Statut;Solution;Articles;Motifs;Montant;Bonus;Rendu;Devise;Suivi;Référence\n");
        UUID cursor = null;
        int pages = 0;
        do {
            List<ReturnView> page = service.search(status, null, cursor, 100);
            for (ReturnView r : page) {
                csv.append(String.join(";",
                        cell(r.number()), cell(r.createdAt().toString()), cell(r.orderName()), cell(r.customerName()),
                        cell(r.customerEmail()), cell(r.status()), cell(r.resolutionType()),
                        cell(r.lines().stream().map(l -> l.title() + " x" + l.quantity()).reduce((a, b) -> a + ", " + b).orElse("")),
                        cell(r.lines().stream().map(ReturnView.Line::reasonCode).distinct().reduce((a, b) -> a + ", " + b).orElse("")),
                        decimal(r.totalMinor()), decimal(r.bonusMinor()), decimal(r.creditMinor()), cell(r.currency()),
                        cell(r.trackingNumber()), cell(r.resolutionRef()))).append('\n');
            }
            cursor = page.size() == 100 ? page.getLast().id() : null;
        } while (cursor != null && ++pages < 500);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"retours.csv\"")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.toString());
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        // Neutralise les formules (injection CSV) et échappe les guillemets.
        String v = value.matches("^[=+\\-@].*") ? "'" + value : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String decimal(long minor) {
        return java.math.BigDecimal.valueOf(minor).movePointLeft(2).toPlainString().replace('.', ',');
    }

    @GetMapping("/{id}")
    ReturnView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/approve")
    ReturnView approve(@PathVariable UUID id) {
        return service.approve(id, Principals.currentActorName());
    }

    @PostMapping("/{id}/reject")
    ReturnView reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest body) {
        return service.reject(id, body.reason(), Principals.currentActorName());
    }

    @PostMapping("/{id}/receive")
    ReturnView receive(@PathVariable UUID id) {
        return service.receive(id, Principals.currentActorName());
    }

    /** Relance l'exécution de la résolution (remboursement, échange, avoir) après un échec. */
    @PostMapping("/{id}/resolve")
    ReturnView resolve(@PathVariable UUID id) {
        return service.retryResolution(id, Principals.currentActorName());
    }

    @PostMapping("/{id}/notes")
    ReturnView addNote(@PathVariable UUID id, @Valid @RequestBody NoteRequest body) {
        return service.addNote(id, Principals.currentActorName(), body.note());
    }
}
