package app.loopa.storage;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/** Façade publique du module storage (tenant courant). */
public interface StorageApi {

    /** Enregistre une image ou un PDF ; refuse les autres types et les fichiers de plus de 10 Mo. */
    StoredFile store(String filename, String contentType, long size, InputStream content);

    Optional<StoredFile> find(UUID id);

    InputStream open(UUID id);
}
