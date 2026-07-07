package com.example.enrollmentservice.dto;

import com.example.enrollmentservice.entity.Course;
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

    public static CourseDTO fromEntity(Course course) {
        return CourseDTO.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .description(course.getDescription())
                .credits(course.getCredits())
                .capacity(course.getCapacity())
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
                .build();
    }
}
