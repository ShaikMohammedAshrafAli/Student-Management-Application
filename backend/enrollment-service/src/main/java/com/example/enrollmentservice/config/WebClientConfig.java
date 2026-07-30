package com.example.enrollmentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${student-service.base-url}")
    private String studentServiceBaseUrl;

    @Value("${course-service.base-url}")
    private String courseServiceBaseUrl;

    @Bean
    public WebClient studentServiceWebClient() {
        return WebClient.builder()
                .baseUrl(studentServiceBaseUrl)
                .build();
    }

    @Bean
    public WebClient courseServiceWebClient() {
        return WebClient.builder()
                .baseUrl(courseServiceBaseUrl)
                .build();
    }
}
