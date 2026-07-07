package com.example.enrollmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the enrollment lifecycle of a student in a course.
 * studentId is a reference only (no FK) - the student's own record
 * lives in student-service; this is the boundary of the bounded context.
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_course", columnNames = {"studentId", "course_id"})
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

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
