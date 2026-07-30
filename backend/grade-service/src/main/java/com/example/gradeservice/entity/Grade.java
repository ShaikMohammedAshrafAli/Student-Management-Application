package com.example.gradeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A finalized grade for one student in one course. References
 * (studentId, courseId, enrollmentId) point at rows owned by
 * student-service, course-service, and enrollment-service respectively -
 * grade-service does not hold foreign keys into another service's
 * database. `credits` and `semester` are snapshotted from course-service
 * at assignment time so GPA calculations remain stable even if a
 * course's credit value changes afterward.
 */
@Entity
@Table(name = "grades", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_course_grade", columnNames = {"studentId", "courseId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long enrollmentId;

    /** Snapshotted from course-service at assignment time (e.g. "FALL2026"). */
    @Column(length = 20)
    private String semester;

    /** Snapshotted from course-service at assignment time. */
    @Column(nullable = false)
    private Integer credits;

    /** 0.0 - 10.0 grade-point scale. */
    @Column(nullable = false)
    private Double gradePoints;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
