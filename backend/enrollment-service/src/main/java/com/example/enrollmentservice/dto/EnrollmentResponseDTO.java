package com.example.enrollmentservice.dto;

import com.example.enrollmentservice.entity.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Aggregated view returned by enrollment-service: enrollment status plus
 * a snapshot of student info (from student-service) and course info
 * (from course-service), both fetched at read time via their respective
 * clients rather than owned locally.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDTO {
    private Long id;
    private Long studentId;
    private StudentDTO student;
    private Long courseId;
    private CourseDTO course;
    private String status;

    /** @deprecated see grade-service for the authoritative grade + GPA/CGPA calculation. */
    @Deprecated
    private Double grade;

    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;

    public static EnrollmentResponseDTO fromEntity(Enrollment enrollment, StudentDTO student, CourseDTO course) {
        return EnrollmentResponseDTO.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .student(student)
                .courseId(enrollment.getCourseId())
                .course(course)
                .status(enrollment.getStatus().name())
                .grade(enrollment.getGrade())
                .enrolledAt(enrollment.getEnrolledAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}
