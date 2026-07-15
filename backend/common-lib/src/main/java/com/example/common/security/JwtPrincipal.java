package com.example.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The authenticated caller, as decoded from a JWT issued by auth-service.
 * This is what SecurityContextHolder's Authentication#getPrincipal()
 * returns in every resource service (student-service, enrollment-service,
 * course-service, grade-service) once JwtAuthenticationFilter runs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtPrincipal {
    private Long userId;
    private String email;
    private String role;
    private Long studentId;

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(role);
    }

    /** True if this principal is a STUDENT whose linked profile matches the given id. */
    public boolean ownsStudentId(Long id) {
        return isStudent() && studentId != null && studentId.equals(id);
    }
}
