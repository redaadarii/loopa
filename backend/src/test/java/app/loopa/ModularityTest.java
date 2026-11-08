package app.loopa;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Échoue si un module utilise les classes internes d'un autre ou si un cycle apparaît.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(LoopaApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }
}
