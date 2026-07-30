package com.example.courseservice.repository;

import com.example.courseservice.entity.Course;
import com.example.courseservice.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);
    Page<Course> findBySemester(String semester, Pageable pageable);
    Page<Course> findByDepartmentIgnoreCase(String department, Pageable pageable);
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
}
