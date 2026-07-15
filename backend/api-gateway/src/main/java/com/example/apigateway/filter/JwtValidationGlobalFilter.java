package com.example.apigateway.filter;

import com.example.common.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Runs before every request is routed. Public endpoints (register, login,
 * refresh, actuator health) pass straight through; everything else must
 * carry a syntactically valid, unexpired Bearer token or the gateway
 * rejects it with 401 before it ever reaches a backend service.
 *
 * This is deliberately a coarse check (signature + expiry only) - the
 * gateway doesn't know about roles/ownership rules for each resource, so
 * fine-grained authorization still happens in the owning service, which
 * independently re-validates the same JWT.
 */
@Component
public class JwtValidationGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/health"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtValidationGlobalFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtTokenValidator = new JwtTokenValidator(jwtSecret);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenValidator.isValid(token)) {
            return reject(exchange, "Invalid or expired token");
        }

        return chain.filter(exchange);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] body = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes();
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
