package com.example.common.constants;

/** Shared JWT / security constants used by auth-service, api-gateway, and resource services. */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String ROLE_CLAIM = "role";
    public static final String USER_ID_CLAIM = "userId";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";

    public static final long ACCESS_TOKEN_EXPIRY_MS = 15 * 60 * 1000L;       // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
}
