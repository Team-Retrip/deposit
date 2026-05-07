package com.deposit.infrastructure.security;

import com.deposit.common.security.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public class AuthenticationFilter extends OncePerRequestFilter {

    private final String publicKey;

    public AuthenticationFilter(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().toLowerCase();

        if (path.equals("/") || path.contains("swagger") || path.contains("api-docs")
                || path.contains("actuator") || path.contains("robots.txt")
                || path.contains("h2-console") || path.contains("status-check")
                || path.startsWith("/alarms")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Claims claims = getClaims(token);
            String subject = claims.getSubject();
            if (subject == null) throw new IllegalStateException("JWT subject is null");
            UUID memberId = UUID.fromString(subject);
            request.setAttribute("userContext", new UserContext(memberId));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || bearerToken.isBlank() || !bearerToken.startsWith("Bearer ")) {
            return null;
        }
        return bearerToken.substring(7);
    }

    private Claims getClaims(String token) {
        try {
            String sanitized = publicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] publicBytes = Base64.getDecoder().decode(sanitized);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(publicBytes));
            return Jwts.parser()
                    .verifyWith(rsaPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new RuntimeException("JWT 파싱 실패", e);
        }
    }
}
