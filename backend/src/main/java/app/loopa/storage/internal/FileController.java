package app.loopa.storage.internal;

import app.loopa.shared.BusinessException;
import app.loopa.storage.StoredFile;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Photos jointes aux retours, visibles par l'équipe du marchand uniquement (filtre tenant). */
@RestController
@Tag(name = "Admin - Fichiers")
class FileController {

    private final FileStorageService storage;

    FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/api/admin/v1/files/{id}")
    ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        StoredFile file = storage.find(id).orElseThrow(() -> BusinessException.notFound("Fichier", id));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(file.filename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(storage.open(id)));
    }
}
