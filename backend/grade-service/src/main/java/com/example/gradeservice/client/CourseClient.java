package com.example.gradeservice.client;

import com.example.gradeservice.dto.CourseDTO;
import com.example.gradeservice.exception.UpstreamServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

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
