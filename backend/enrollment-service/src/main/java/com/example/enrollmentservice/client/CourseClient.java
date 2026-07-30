package com.example.enrollmentservice.client;

import com.example.enrollmentservice.dto.CourseDTO;
import com.example.enrollmentservice.exception.UpstreamServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

/**
 * Encapsulates all HTTP calls from enrollment-service to course-service.
 * Mirrors StudentClient's pattern exactly: forwards the caller's own
 * bearer token downstream (pass-through auth) rather than using a
 * service-account credential.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseClient {

    private final WebClient courseServiceWebClient;

    public Optional<CourseDTO> getCourseById(Long courseId, String bearerToken) {
        try {
            CourseDTO course = courseServiceWebClient.get()
                    .uri("/api/v1/courses/{id}", courseId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(CourseDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(course);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        } catch (WebClientResponseException.Forbidden | WebClientResponseException.Unauthorized ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to reach course-service for id {}: {}", courseId, ex.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Unable to verify course " + courseId + " - course-service is unreachable");
        }
    }
}
