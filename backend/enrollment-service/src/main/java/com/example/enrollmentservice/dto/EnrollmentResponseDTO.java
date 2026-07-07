package com.example.enrollmentservice.dto;

import com.example.enrollmentservice.entity.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregated view returned by enrollment-service: enrollment status plus
 * the course info owned locally and (optionally) a snapshot of student
 * info fetched from student-service at read time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDTO {
    private Long id;
    private Long studentId;
    private StudentDTO student;
    private CourseDTO course;
    private String status;
    private Double grade;
    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;

    public static EnrollmentResponseDTO fromEntity(Enrollment enrollment, StudentDTO student) {
        return EnrollmentResponseDTO.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .student(student)
                .course(CourseDTO.fromEntity(enrollment.getCourse()))
                .status(enrollment.getStatus().name())
                .grade(enrollment.getGrade())
                .enrolledAt(enrollment.getEnrolledAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}
