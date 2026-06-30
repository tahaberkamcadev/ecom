package com.tahaberkamcadev.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Value("${app.gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("Incoming request: {} {}", method, path);

        if (isPublicPath(path, method)) {
            log.debug("Public path, skipping JWT check: {} {}", method, path);
            MutableHttpServletRequest mutableRequest = stripUntrustedHeaders(request);
            mutableRequest.putHeader("X-Gateway-Secret", gatewaySecret);
            chain.doFilter(mutableRequest, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing Bearer token for protected path: {} {}", method, path);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        String token = authHeader.substring(7).strip();
        if (token.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            Claims claims = jwtUtil.parseAndValidate(token);

            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            if ("POST".equalsIgnoreCase(method) && "/api/products".equals(path) && !"ADMIN".equals(role)) {
                sendForbiddenResponse(response, "Admin role required");
                return;
            }

            MutableHttpServletRequest mutableRequest = stripUntrustedHeaders(request);
            mutableRequest.putHeader("X-Gateway-Secret", gatewaySecret);
            mutableRequest.putHeader("X-User-Email", email);
            if (userId != null) {
                mutableRequest.putHeader("X-User-Id", userId);
            }
            if (role != null) {
                mutableRequest.putHeader("X-User-Role", role);
            }

            log.debug("JWT validated for user: {}, forwarding to upstream", email);
            chain.doFilter(mutableRequest, response);

        } catch (JwtException e) {
            log.warn("JWT validation failed for [{} {}]: {}", method, path, e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private boolean isPublicPath(String path, String method) {
        return path.startsWith("/api/v1/auth/")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info")
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/api/catalog/products"));
    }

    private MutableHttpServletRequest stripUntrustedHeaders(HttpServletRequest request) {
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
        mutableRequest.removeHeader("X-User-Id");
        mutableRequest.removeHeader("X-User-Role");
        mutableRequest.removeHeader("X-User-Email");
        mutableRequest.removeHeader("X-Gateway-Secret");
        return mutableRequest;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}"
        );
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"Forbidden\",\"message\":\"" + message + "\"}"
        );
    }
}
