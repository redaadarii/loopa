package app.loopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée de Loopa.
 * <p>
 * Monolithe modulaire : chaque sous-package direct de {@code app.loopa} est un module
 * applicatif vérifié par Spring Modulith (voir {@code ModularityTest}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LoopaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoopaApplication.class, args);
    }
}
