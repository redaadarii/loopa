package app.loopa.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Une chaîne de sécurité par surface :
 * <ul>
 *   <li>{@code /webhooks/**}, {@code /auth/shopify/**} : publics, authenticité vérifiée par HMAC Shopify ;</li>
 *   <li>{@code /api/**} : jeton Bearer (marchand {@code admin}/{@code agent} ou client {@code portal}) ;</li>
 *   <li>le reste : santé et documentation OpenAPI.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain shopifyEndpoints(HttpSecurity http) throws Exception {
        return http.securityMatcher("/webhooks/**", "/auth/shopify/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())          // API sans cookie : jeton Bearer uniquement
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/portal/v1/shops/**", "/api/portal/v1/sessions/**").permitAll()
                        .requestMatchers("/api/admin/v1/team/**", "/api/admin/v1/billing/**",
                                "/api/admin/v1/integrations/**").hasAuthority("SCOPE_admin")
                        .requestMatchers("/api/admin/**", "/api/auth/me", "/api/auth/password").hasAnyAuthority("SCOPE_admin", "SCOPE_agent")
                        .requestMatchers("/api/portal/**").hasAuthority("SCOPE_portal")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                .addFilterAfter(new TenantFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain technical(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.corsOrigins());
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
