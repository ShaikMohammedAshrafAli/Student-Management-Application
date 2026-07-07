package com.example.enrollmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the data contract exposed by student-service's /api/v1/students
 * endpoint. Only the fields enrollment-service actually needs are kept.
 * This is the "clearly defined data contract" boundary between the two
 * services - both sides evolve independently as long as this shape holds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
}
