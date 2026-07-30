package com.example.gradeservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${course-service.base-url}")
    private String courseServiceBaseUrl;

    @Value("${enrollment-service.base-url:http://localhost:8082}")
    private String enrollmentServiceBaseUrl;

    @Bean
    public WebClient courseServiceWebClient() {
        return WebClient.builder()
                .baseUrl(courseServiceBaseUrl)
                .build();
    }

    @Bean
    public WebClient enrollmentServiceWebClient() {
        return WebClient.builder()
                .baseUrl(enrollmentServiceBaseUrl)
                .build();
    }
}
