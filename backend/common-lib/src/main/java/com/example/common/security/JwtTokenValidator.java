package com.example.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Validates and decodes JWTs issued by auth-service. Every resource
 * service constructs one of these (as a @Bean, seeded with the same
 * shared jwt.secret auth-service uses) instead of re-implementing JWT
 * parsing locally.
 *
 * This class is intentionally framework-agnostic (no servlet/reactive
 * dependency) so it works from both a servlet-stack filter (student-service,
 * enrollment-service) and a WebFlux GlobalFilter (api-gateway).
 */
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public boolean isValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public JwtPrincipal extractPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        Long studentId = claims.get("studentId", Long.class);
        return JwtPrincipal.builder()
                .userId(claims.get("userId", Long.class))
                .email(claims.getSubject())
                .role(claims.get("role", String.class))
                .studentId(studentId)
                .build();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
