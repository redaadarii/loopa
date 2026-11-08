package app.loopa.security;

import app.loopa.shared.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Place la boutique du jeton dans le {@link TenantContext} et le nettoie systématiquement à la fin.
 * Les routes publiques (portail avant OTP, webhooks) fixent le tenant elles-mêmes avec
 * {@link TenantContext#callAs}.
 */
class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Principals.shopIdIfPresent().ifPresent(TenantContext::set);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
