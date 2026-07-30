package com.example.gradeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the fields grade-service needs from enrollment-service's
 * EnrollmentResponseDTO - the same cross-service DTO pattern used
 * throughout this system (see StudentDTO / CourseDTO in enrollment-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String status;
}
