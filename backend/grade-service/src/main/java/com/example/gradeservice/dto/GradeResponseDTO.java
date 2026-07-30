package com.example.gradeservice.dto;

import com.example.gradeservice.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponseDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Long enrollmentId;
    private String semester;
    private Integer credits;
    private Double gradePoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GradeResponseDTO fromEntity(Grade grade, CourseDTO course) {
        return GradeResponseDTO.builder()
                .id(grade.getId())
                .studentId(grade.getStudentId())
                .courseId(grade.getCourseId())
                .courseCode(course != null ? course.getCourseCode() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .enrollmentId(grade.getEnrollmentId())
                .semester(grade.getSemester())
                .credits(grade.getCredits())
                .gradePoints(grade.getGradePoints())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }
}
