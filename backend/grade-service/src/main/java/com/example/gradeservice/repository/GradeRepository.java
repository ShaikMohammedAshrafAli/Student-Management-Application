package com.example.gradeservice.repository;

import com.example.gradeservice.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByStudentIdAndSemester(Long studentId, String semester);

    Optional<Grade> findByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Grade> findByEnrollmentId(Long enrollmentId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
