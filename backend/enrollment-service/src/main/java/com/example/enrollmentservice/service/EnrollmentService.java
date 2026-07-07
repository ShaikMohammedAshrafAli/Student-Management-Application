package com.example.enrollmentservice.service;

import com.example.enrollmentservice.client.StudentClient;
import com.example.enrollmentservice.dto.EnrollmentRequestDTO;
import com.example.enrollmentservice.dto.EnrollmentResponseDTO;
import com.example.enrollmentservice.dto.StudentDTO;
import com.example.enrollmentservice.entity.Course;
import com.example.enrollmentservice.entity.Enrollment;
import com.example.enrollmentservice.exception.CourseCapacityExceededException;
import com.example.enrollmentservice.exception.DuplicateResourceException;
import com.example.enrollmentservice.exception.ResourceNotFoundException;
import com.example.enrollmentservice.repository.CourseRepository;
import com.example.enrollmentservice.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the student <-> course enrollment lifecycle.
 * This is where the inter-service REST call happens: before an enrollment
 * is created, the student's existence is validated against student-service.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentClient studentClient;

    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO request) {
        // 1. Validate the course exists and has room.
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        long confirmedCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), Enrollment.EnrollmentStatus.CONFIRMED);
        if (confirmedCount >= course.getCapacity()) {
            throw new CourseCapacityExceededException(
                    "Course '" + course.getCourseCode() + "' has reached its capacity of " + course.getCapacity());
        }

        // 2. Validate the student exists via inter-service REST call to student-service.
        StudentDTO student = studentClient.getStudentById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + request.getStudentId()));

        // 3. Prevent duplicate enrollment.
        if (enrollmentRepository.existsByStudentIdAndCourseId(request.getStudentId(), request.getCourseId())) {
            throw new DuplicateResourceException(
                    "Student " + request.getStudentId() + " is already enrolled in course " + request.getCourseId());
        }

        // 4. Persist the enrollment as CONFIRMED (workflow could route through
        //    PENDING -> approval step first; kept simple here).
        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .course(course)
                .status(Enrollment.EnrollmentStatus.CONFIRMED)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        StudentDTO student = studentClient.getStudentById(enrollment.getStudentId()).orElse(null);
        return EnrollmentResponseDTO.fromEntity(enrollment, student);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId) {
        StudentDTO student = studentClient.getStudentById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(e -> EnrollmentResponseDTO.fromEntity(e, student))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> {
                    StudentDTO student = studentClient.getStudentById(e.getStudentId()).orElse(null);
                    return EnrollmentResponseDTO.fromEntity(e, student);
                })
                .toList();
    }

    public EnrollmentResponseDTO updateStatus(Long enrollmentId, String status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setStatus(Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
        Enrollment saved = enrollmentRepository.save(enrollment);
        StudentDTO student = studentClient.getStudentById(saved.getStudentId()).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    public EnrollmentResponseDTO recordGrade(Long enrollmentId, Double grade) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setGrade(grade);
        enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        Enrollment saved = enrollmentRepository.save(enrollment);
        StudentDTO student = studentClient.getStudentById(saved.getStudentId()).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    public void dropEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
    }
}
