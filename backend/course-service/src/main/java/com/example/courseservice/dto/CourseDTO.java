package com.example.courseservice.dto;

import com.example.courseservice.entity.Course;
import com.example.courseservice.entity.CourseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Long id;

    @NotBlank(message = "Course code is required")
    private String courseCode;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Credits is required")
    @Min(value = 1, message = "Credits must be at least 1")
    private Integer credits;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private String semester;
    private String instructor;
    private String department;
    private String status;

    public static CourseDTO fromEntity(Course course) {
        return CourseDTO.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .description(course.getDescription())
                .credits(course.getCredits())
                .capacity(course.getCapacity())
                .semester(course.getSemester())
                .instructor(course.getInstructor())
                .department(course.getDepartment())
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .build();
    }

    public Course toEntity() {
        return Course.builder()
                .id(this.id)
                .courseCode(this.courseCode)
                .title(this.title)
                .description(this.description)
                .credits(this.credits)
                .capacity(this.capacity != null ? this.capacity : 30)
                .semester(this.semester)
                .instructor(this.instructor)
                .department(this.department)
                .status(this.status != null ? CourseStatus.valueOf(this.status.toUpperCase()) : CourseStatus.ACTIVE)
                .build();
    }
}
