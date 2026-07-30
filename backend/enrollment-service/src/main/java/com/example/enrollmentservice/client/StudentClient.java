package com.example.enrollmentservice.client;

import com.example.enrollmentservice.dto.StudentDTO;
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
 * Encapsulates all HTTP calls from enrollment-service to student-service.
 * Keeping this in a single client class means the rest of the codebase
 * never talks HTTP directly - it only knows about StudentDTO.
 *
 * Every method takes the caller's original Bearer token and forwards it
 * downstream ("pass-through auth"), rather than enrollment-service having
 * its own service-account credentials. That way student-service applies
 * the SAME ownership rule it would for a direct call: a STUDENT token can
 * only read their own profile, an ADMIN token can read anyone's.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentClient {

    private final WebClient studentServiceWebClient;

    /**
     * Fetches the full student profile using the caller's own bearer token.
     * Returns empty if the student does not exist (404 from student-service).
     */
    public Optional<StudentDTO> getStudentById(Long studentId, String bearerToken) {
        try {
            StudentDTO student = studentServiceWebClient.get()
                    .uri("/api/v1/students/{id}", studentId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(StudentDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(student);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        } catch (WebClientResponseException.Forbidden | WebClientResponseException.Unauthorized ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to reach student-service for id {}: {}", studentId, ex.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Unable to verify student " + studentId + " - student-service is unreachable");
        }
    }

    /**
     * Cheap existence check, used before creating an enrollment,
     * so we don't have to deserialize a full profile just to validate.
     */
    public boolean studentExists(Long studentId, String bearerToken) {
        try {
            Boolean exists = studentServiceWebClient.get()
                    .uri("/api/v1/students/{id}/exists", studentId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.error("Failed to reach student-service to check existence of {}: {}", studentId, ex.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Unable to verify student " + studentId + " - student-service is unreachable");
        }
    }
}
