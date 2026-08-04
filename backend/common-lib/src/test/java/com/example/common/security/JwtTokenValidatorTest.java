package com.example.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the JWT parsing/validation logic shared by every
 * resource service (student-service, enrollment-service, course-service,
 * grade-service) and the gateway. No Spring context is needed here -
 * JwtTokenValidator is a plain class, which is exactly why it's easy to
 * unit test in isolation.
 */
class JwtTokenValidatorTest {

    // Same shared secret pattern used across the system's application.properties (local-dev only).
    private static final String SECRET = "MzM0NmM4NDNhOWM5NDcyM2FhZmY3NmY2ZGRhYTQ5ZjM0Njc4OTBhYmNkZWYxMjM0NTY3ODkwYWJjZGVm";

    private final JwtTokenValidator validator = new JwtTokenValidator(SECRET);

    private String buildToken(String email, String role, Long userId, Long studentId, long expiryOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);
        claims.put("studentId", studentId);

        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryOffsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    void isValid_returnsTrue_forAWellFormedUnexpiredToken() {
        String token = buildToken("asha@example.com", "STUDENT", 1L, 42L, 15 * 60 * 1000);

        assertThat(validator.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forAnExpiredToken() {
        // Expired one minute ago.
        String token = buildToken("asha@example.com", "STUDENT", 1L, 42L, -60_000);

        assertThat(validator.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forATokenSignedWithADifferentSecret() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "differentsecretdifferentsecretdifferentsecretdifferentsecret12".getBytes());
        String token = Jwts.builder()
                .subject("asha@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();

        assertThat(validator.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forGarbageInput() {
        assertThat(validator.isValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void extractPrincipal_decodesAllClaimsCorrectly_forAStudentToken() {
        String token = buildToken("asha@example.com", "STUDENT", 7L, 42L, 15 * 60 * 1000);

        JwtPrincipal principal = validator.extractPrincipal(token);

        assertThat(principal.getEmail()).isEqualTo("asha@example.com");
        assertThat(principal.getUserId()).isEqualTo(7L);
        assertThat(principal.getStudentId()).isEqualTo(42L);
        assertThat(principal.getRole()).isEqualTo("STUDENT");
        assertThat(principal.isStudent()).isTrue();
        assertThat(principal.isAdmin()).isFalse();
    }

    @Test
    void extractPrincipal_forAnAdminToken_hasNoStudentId() {
        String token = buildToken("admin@example.com", "ADMIN", 1L, null, 15 * 60 * 1000);

        JwtPrincipal principal = validator.extractPrincipal(token);

        assertThat(principal.isAdmin()).isTrue();
        assertThat(principal.getStudentId()).isNull();
    }

    @Test
    void jwtPrincipal_ownsStudentId_onlyMatchesForTheStudentsOwnLinkedId() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(42L).build();

        assertThat(student.ownsStudentId(42L)).isTrue();
        assertThat(student.ownsStudentId(99L)).isFalse();
    }

    @Test
    void jwtPrincipal_ownsStudentId_isAlwaysFalseForAdmins() {
        // Ownership is a STUDENT-only concept - an ADMIN's access comes from
        // isAdmin() checks in each service, not from "owning" a student id.
        JwtPrincipal admin = JwtPrincipal.builder().role("ADMIN").studentId(null).build();

        assertThat(admin.ownsStudentId(42L)).isFalse();
    }
}
