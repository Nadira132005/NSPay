package com.nspay.config;
import com.nspay.services.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "app.jwt", name = "filter-enabled", havingValue = "true", matchIfMissing = true)//// Loads this component only if app.jwt.filter-enabled is 'true'
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_TOKEN_COOKIE = "auth_token";
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return path.startsWith("/api/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token == null || !jwtService.isTokenValid(token)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid auth token");
            return;
        }

        Claims claims = jwtService.parseToken(token);
        boolean passwordChangeRequired = Boolean.TRUE.equals(claims.get("passwordChangeRequired", Boolean.class));
        if (passwordChangeRequired && !("PUT".equalsIgnoreCase(request.getMethod())
                && "/api/users/me/password".equals(request.getRequestURI()))) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Password change is required before using this account");
            return;
        }
        request.setAttribute("authenticatedUserId", claims.get("userId"));
        request.setAttribute("passwordChangeRequired", passwordChangeRequired);
        request.setAttribute("authenticatedRole", claims.get("role"));
        request.setAttribute("authenticatedName", claims.getSubject());

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (AUTH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
