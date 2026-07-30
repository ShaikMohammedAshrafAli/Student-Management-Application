package com.example.courseservice.service;

import com.example.courseservice.dto.CourseDTO;
import com.example.courseservice.entity.Course;
import com.example.courseservice.exception.DuplicateResourceException;
import com.example.courseservice.exception.ResourceNotFoundException;
import com.example.courseservice.repository.CourseRepository;
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
        return CourseDTO.fromEntity(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream().map(CourseDTO::fromEntity).toList();
    }

    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        Course existing = findOrThrow(id);

        if (dto.getCourseCode() != null && !dto.getCourseCode().equalsIgnoreCase(existing.getCourseCode())
                && courseRepository.existsByCourseCode(dto.getCourseCode())) {
            throw new DuplicateResourceException("Course with code '" + dto.getCourseCode() + "' already exists");
        }

        if (dto.getCourseCode() != null) existing.setCourseCode(dto.getCourseCode());
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getCredits() != null) existing.setCredits(dto.getCredits());
        if (dto.getCapacity() != null) existing.setCapacity(dto.getCapacity());
        if (dto.getSemester() != null) existing.setSemester(dto.getSemester());
        if (dto.getInstructor() != null) existing.setInstructor(dto.getInstructor());
        if (dto.getDepartment() != null) existing.setDepartment(dto.getDepartment());
        if (dto.getStatus() != null) existing.setStatus(com.example.courseservice.entity.CourseStatus.valueOf(dto.getStatus().toUpperCase()));

        return CourseDTO.fromEntity(courseRepository.save(existing));
    }

    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        courseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return courseRepository.existsById(id);
    }

    private Course findOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }
}
