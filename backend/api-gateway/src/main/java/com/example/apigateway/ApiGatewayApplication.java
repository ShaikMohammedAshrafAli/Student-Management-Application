package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for all clients (the React frontend, Postman, etc).
 * Routes requests to the correct downstream service and rejects requests
 * with a missing/invalid/expired JWT before they ever reach a backend
 * service - defense in depth alongside each service's own JWT filter.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
