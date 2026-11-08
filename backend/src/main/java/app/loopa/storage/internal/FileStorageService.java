package app.loopa.storage.internal;

import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.storage.StorageApi;
import app.loopa.storage.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Table(name = "stored_file")
class StoredFileEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @Column(nullable = false)
    String filename;

    @Column(name = "content_type", nullable = false)
    String contentType;

    @Column(nullable = false)
    long size;

    @Column(name = "storage_key", nullable = false)
    String storageKey;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected StoredFileEntity() {
    }

    StoredFile toView() {
        return new StoredFile(id, filename, contentType, size, createdAt);
    }
}

interface StoredFileRepository extends JpaRepository<StoredFileEntity, UUID> {
}

@Service
class FileStorageService implements StorageApi {

    static final long MAX_SIZE = 10L * 1024 * 1024;
    static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/heic",
            "image/heif", "application/pdf");

    private final StoredFileRepository files;
    private final Path root;
    private final Clock clock;

    FileStorageService(StoredFileRepository files, @Value("${loopa.storage.dir:./data/uploads}") String dir,
                       Clock clock) {
        this.files = files;
        this.root = Path.of(dir).toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    @Transactional
    public StoredFile store(String filename, String contentType, long size, InputStream content) {
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new BusinessException("UNSUPPORTED_FILE", HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formats acceptés : JPEG, PNG, WebP, HEIC ou PDF.");
        }
        if (size <= 0 || size > MAX_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE,
                    "Le fichier doit faire moins de 10 Mo.");
        }
        UUID tenant = TenantContext.require();
        UUID id = UUID.randomUUID();
        // Nom de stockage aléatoire : le nom d'origine n'est jamais utilisé comme chemin.
        String key = tenant + "/" + id;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Chemin de stockage invalide");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du fichier impossible", e);
        }
        StoredFileEntity e = new StoredFileEntity();
        e.id = id;
        e.tenantId = tenant;
        e.filename = sanitize(filename);
        e.contentType = contentType;
        e.size = size;
        e.storageKey = key;
        e.createdAt = clock.instant();
        return files.save(e).toView();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> find(UUID id) {
        return files.findById(id).map(StoredFileEntity::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream open(UUID id) {
        StoredFileEntity e = files.findById(id).orElseThrow(() -> BusinessException.notFound("Fichier", id));
        try {
            return Files.newInputStream(root.resolve(e.storageKey).normalize());
        } catch (IOException ex) {
            throw new UncheckedIOException("Lecture du fichier impossible", ex);
        }
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "fichier";
        }
        String name = Path.of(filename).getFileName().toString().replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }
}
