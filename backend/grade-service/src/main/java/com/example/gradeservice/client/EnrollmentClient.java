package com.example.gradeservice.client;

import com.example.gradeservice.dto.EnrollmentDTO;
import com.example.gradeservice.exception.UpstreamServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

/**
 * Encapsulates all HTTP calls from grade-service to enrollment-service.
 * Forwards the caller's own bearer token downstream (pass-through auth),
 * same pattern as StudentClient/CourseClient in enrollment-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentClient {

    private final WebClient enrollmentServiceWebClient;

    public Optional<EnrollmentDTO> getEnrollmentById(Long enrollmentId, String bearerToken) {
        try {
            EnrollmentDTO enrollment = enrollmentServiceWebClient.get()
                    .uri("/api/v1/enrollments/{id}", enrollmentId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(EnrollmentDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(enrollment);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        } catch (WebClientResponseException.Forbidden | WebClientResponseException.Unauthorized ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to reach enrollment-service for id {}: {}", enrollmentId, ex.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Unable to verify enrollment " + enrollmentId + " - enrollment-service is unreachable");
        }
    }
}
