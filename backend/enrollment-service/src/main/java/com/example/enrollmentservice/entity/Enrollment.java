package com.example.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the enrollment lifecycle of a student in a course.
 * Both studentId and courseId are references only (no FK, no JPA
 * relation) - the student record lives in student-service and the
 * course record lives in course-service. This is the bounded-context
 * boundary: enrollment-service owns only the enrollment fact itself.
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_course", columnNames = {"studentId", "courseId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference to the student owned by student-service. */
    @Column(nullable = false)
    private Long studentId;

    /** Reference to the course owned by course-service. */
    @Column(nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    /**
     * @deprecated retained for backward compatibility with existing rows
     * and the legacy PATCH /enrollments/{id}/grade endpoint. grade-service
     * is now the authoritative source for grades and GPA/CGPA calculation.
     */
    @Deprecated
    private Double grade;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.enrolledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enrollment lifecycle states - mirrors a typical course-registration
     * workflow: a request is PENDING, becomes CONFIRMED (or REJECTED) once
     * capacity/student validation passes, may later be DROPPED or COMPLETED.
     */
    public enum EnrollmentStatus {
        PENDING, CONFIRMED, REJECTED, DROPPED, COMPLETED
    }
}
