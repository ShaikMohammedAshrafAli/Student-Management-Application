package com.example.enrollmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the data contract exposed by course-service's /api/v1/courses
 * endpoint - the same cross-service DTO pattern used for StudentDTO.
 * Only the fields enrollment-service actually needs are kept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String courseCode;
    private String title;
    private Integer credits;
    private Integer capacity;
    private String semester;
    private String instructor;
    private String department;
    private String status;
}
