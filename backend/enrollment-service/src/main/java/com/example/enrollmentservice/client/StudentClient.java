package com.example.enrollmentservice.client;

import com.example.enrollmentservice.dto.StudentDTO;
import com.example.enrollmentservice.exception.StudentServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Encapsulates all HTTP calls from enrollment-service to student-service.
 * Keeping this in a single client class means the rest of the codebase
 * never talks HTTP directly - it only knows about StudentDTO.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentClient {

    private final WebClient studentServiceWebClient;

    /**
     * Fetches the full student profile. Returns empty if the student
     * does not exist (404 from student-service).
     */
    public Optional<StudentDTO> getStudentById(Long studentId) {
        try {
            StudentDTO student = studentServiceWebClient.get()
                    .uri("/api/v1/students/{id}", studentId)
                    .retrieve()
                    .bodyToMono(StudentDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Optional.ofNullable(student);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to reach student-service for id {}: {}", studentId, ex.getMessage());
            throw new StudentServiceUnavailableException(
                    "Unable to verify student " + studentId + " - student-service is unreachable");
        }
    }

    /**
     * Cheap existence check, used before creating an enrollment,
     * so we don't have to deserialize a full profile just to validate.
     */
    public boolean studentExists(Long studentId) {
        try {
            Boolean exists = studentServiceWebClient.get()
                    .uri("/api/v1/students/{id}/exists", studentId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.error("Failed to reach student-service to check existence of {}: {}", studentId, ex.getMessage());
            throw new StudentServiceUnavailableException(
                    "Unable to verify student " + studentId + " - student-service is unreachable");
        }
    }
}
