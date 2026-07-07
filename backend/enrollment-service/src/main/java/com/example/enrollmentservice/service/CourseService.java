package com.example.enrollmentservice.service;

import com.example.enrollmentservice.dto.CourseDTO;
import com.example.enrollmentservice.entity.Course;
import com.example.enrollmentservice.exception.DuplicateResourceException;
import com.example.enrollmentservice.exception.ResourceNotFoundException;
import com.example.enrollmentservice.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseDTO createCourse(CourseDTO dto) {
        if (courseRepository.existsByCourseCode(dto.getCourseCode())) {
            throw new DuplicateResourceException("Course with code '" + dto.getCourseCode() + "' already exists");
        }
        Course course = dto.toEntity();
        course.setId(null);
        return CourseDTO.fromEntity(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return CourseDTO.fromEntity(course);
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream().map(CourseDTO::fromEntity).toList();
    }

    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        Course existing = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (dto.getCourseCode() != null) existing.setCourseCode(dto.getCourseCode());
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getCredits() != null) existing.setCredits(dto.getCredits());
        if (dto.getCapacity() != null) existing.setCapacity(dto.getCapacity());

        return CourseDTO.fromEntity(courseRepository.save(existing));
    }

    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    /** Package-private helper for EnrollmentService to fetch the raw entity. */
    @Transactional(readOnly = true)
    Course getCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }
}
