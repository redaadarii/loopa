package app.loopa.shared;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Écouteurs d'événements asynchrones (après commit) et tâches planifiées.
 * Avec les threads virtuels activés, Spring Boot fournit un exécuteur virtuel par défaut.
 */
@Configuration
@EnableAsync
@EnableScheduling
class AsyncConfig {
}
