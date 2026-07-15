package com.example.enrollmentservice.service;

import com.example.common.security.JwtPrincipal;
import com.example.common.security.SecurityUtils;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates the student <-> course enrollment lifecycle.
 *
 * Access rules enforced here (in addition to @PreAuthorize on the
 * controller for admin-only actions):
 * - A STUDENT may only enroll THEMSELVES (their token's studentId is used,
 *   overriding whatever studentId was sent in the request body) and may
 *   only view/drop their OWN enrollments.
 * - An ADMIN may enroll/view/drop on behalf of any student.
 *
 * Every call to student-service forwards the original caller's bearer
 * token, so student-service applies the exact same ownership rule.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final StudentClient studentClient;

    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO request, String bearerToken) {
        JwtPrincipal principal = requirePrincipal();

        // Determine the effective student ID exactly once
        final Long effectiveStudentId;

        if (principal.isAdmin()) {
            effectiveStudentId = request.getStudentId();
        } else {
            effectiveStudentId = principal.getStudentId();
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found with id: " + request.getCourseId()));

        long confirmedCount = enrollmentRepository.countByCourseIdAndStatus(
                course.getId(),
                Enrollment.EnrollmentStatus.CONFIRMED);

        if (confirmedCount >= course.getCapacity()) {
            throw new CourseCapacityExceededException(
                    "Course '" + course.getCourseCode()
                            + "' has reached its capacity of "
                            + course.getCapacity());
        }

        StudentDTO student = studentClient.getStudentById(effectiveStudentId, bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with id: " + effectiveStudentId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                effectiveStudentId,
                request.getCourseId())) {

            throw new DuplicateResourceException(
                    "Student " + effectiveStudentId
                            + " is already enrolled in course "
                            + request.getCourseId());
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(effectiveStudentId)
                .course(course)
                .status(Enrollment.EnrollmentStatus.CONFIRMED)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id, String bearerToken) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        requireAdminOrOwner(enrollment.getStudentId());
        StudentDTO student = studentClient.getStudentById(enrollment.getStudentId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(enrollment, student);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId, String bearerToken) {
        requireAdminOrOwner(studentId);
        StudentDTO student = studentClient.getStudentById(studentId, bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(e -> EnrollmentResponseDTO.fromEntity(e, student))
                .toList();
    }

    /** ADMIN only - see EnrollmentController's @PreAuthorize. */
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByCourse(Long courseId, String bearerToken) {
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> {
                    StudentDTO student = studentClient.getStudentById(e.getStudentId(), bearerToken).orElse(null);
                    return EnrollmentResponseDTO.fromEntity(e, student);
                })
                .toList();
    }

    /** ADMIN only - see EnrollmentController's @PreAuthorize. */
    public EnrollmentResponseDTO updateStatus(Long enrollmentId, String status, String bearerToken) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setStatus(Enrollment.EnrollmentStatus.valueOf(status.toUpperCase()));
        Enrollment saved = enrollmentRepository.save(enrollment);
        StudentDTO student = studentClient.getStudentById(saved.getStudentId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    /** ADMIN only - see EnrollmentController's @PreAuthorize. */
    public EnrollmentResponseDTO recordGrade(Long enrollmentId, Double grade, String bearerToken) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setGrade(grade);
        enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        Enrollment saved = enrollmentRepository.save(enrollment);
        StudentDTO student = studentClient.getStudentById(saved.getStudentId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student);
    }

    public void dropEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        requireAdminOrOwner(enrollment.getStudentId());
        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
    }

    private JwtPrincipal requirePrincipal() {
        JwtPrincipal principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }

    private void requireAdminOrOwner(Long studentId) {
        JwtPrincipal principal = requirePrincipal();
        if (!principal.isAdmin() && !principal.ownsStudentId(studentId)) {
            throw new AccessDeniedException("You may only access your own enrollments");
        }
    }
}
