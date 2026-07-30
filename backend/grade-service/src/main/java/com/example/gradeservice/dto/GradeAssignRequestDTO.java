package com.example.gradeservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An ADMIN assigns a grade against an existing enrollment (not raw
 * studentId/courseId) - this guarantees a grade can only be recorded for
 * a real, verified enrollment, and studentId/courseId/semester/credits
 * are all derived server-side from that enrollment and its course.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeAssignRequestDTO {

    @NotNull(message = "enrollmentId is required")
    private Long enrollmentId;

    @NotNull(message = "gradePoints is required")
    @DecimalMin(value = "0.0", message = "gradePoints must be between 0 and 10")
    @DecimalMax(value = "10.0", message = "gradePoints must be between 0 and 10")
    private Double gradePoints;
}
