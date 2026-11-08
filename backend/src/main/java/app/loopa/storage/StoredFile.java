package app.loopa.storage;

import java.time.Instant;
import java.util.UUID;

public record StoredFile(UUID id, String filename, String contentType, long size, Instant createdAt) {
}
