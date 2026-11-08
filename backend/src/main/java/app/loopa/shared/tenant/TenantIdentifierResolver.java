package app.loopa.shared.tenant;

import java.util.Map;
import java.util.UUID;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Branche le {@link TenantContext} sur le support multi-tenant d'Hibernate :
 * toute entité avec un champ {@code @TenantId} est automatiquement filtrée et renseignée.
 * La base ajoute une seconde barrière avec Row Level Security (voir V1__init.sql).
 */
@Component
class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.current().orElse(TenantContext.SYSTEM);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
