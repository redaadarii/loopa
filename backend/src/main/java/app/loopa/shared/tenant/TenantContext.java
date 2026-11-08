package app.loopa.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant (boutique) courant, porté par le thread de la requête.
 * Alimenté par {@code TenantFilter}, lu par Hibernate ({@code @TenantId}) pour filtrer toutes les requêtes.
 */
public final class TenantContext {

    /** Valeur utilisée hors requête (jobs système, migrations) : ne correspond à aucune boutique. */
    public static final UUID SYSTEM = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static UUID require() {
        return current().orElseThrow(() -> new IllegalStateException("Aucun tenant dans le contexte"));
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Exécute {@code action} pour le tenant donné puis restaure le contexte précédent.
     * À appeler AVANT d'entrer dans une méthode {@code @Transactional} : Hibernate lit le tenant
     * à l'ouverture de la session, c'est-à-dire au début de la transaction.
     */
    public static <T> T callAs(UUID tenantId, java.util.function.Supplier<T> action) {
        UUID previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(UUID tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
