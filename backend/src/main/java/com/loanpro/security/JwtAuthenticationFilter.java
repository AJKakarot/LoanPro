package com.loanpro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityResponseWriter securityResponseWriter;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            SecurityResponseWriter securityResponseWriter
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityResponseWriter = securityResponseWriter;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            if (isPublic(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = jwtService.parseAccessToken(token);
                    UUID userId = jwtService.parseUserId(claims);
                    int tokenVersion = jwtService.parseTokenVersion(claims);
                    var principal = (UserPrincipal) userDetailsService.loadUserById(userId);
                    if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
                        securityResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Account is not active");
                        return;
                    }
                    if (principal.getTokenVersion() != tokenVersion) {
                        securityResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Token is no longer valid");
                        return;
                    }
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (JwtException | IllegalArgumentException ex) {
                    if (!isPublic(request)) {
                        securityResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                        return;
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh".equals(path)
                || "/api/v1/auth/logout".equals(path)
                || "/actuator/health".equals(path);
    }
}
