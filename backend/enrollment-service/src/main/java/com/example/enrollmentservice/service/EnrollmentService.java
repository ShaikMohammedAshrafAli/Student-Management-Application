package com.example.enrollmentservice.service;

import com.example.common.security.JwtPrincipal;
import com.example.common.security.SecurityUtils;
import com.example.enrollmentservice.client.CourseClient;
import com.example.enrollmentservice.client.StudentClient;
import com.example.enrollmentservice.dto.CourseDTO;
import com.example.enrollmentservice.dto.EnrollmentRequestDTO;
import com.example.enrollmentservice.dto.EnrollmentResponseDTO;
import com.example.enrollmentservice.dto.StudentDTO;
import com.example.enrollmentservice.entity.Enrollment;
import com.example.enrollmentservice.exception.CourseCapacityExceededException;
import com.example.enrollmentservice.exception.DuplicateResourceException;
import com.example.enrollmentservice.exception.ResourceNotFoundException;
import com.example.enrollmentservice.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates the student <-> course enrollment lifecycle.
 *
 * As of Phase 3, enrollment-service owns only the enrollment fact itself
 * (studentId + courseId + status/grade) - both the student profile and
 * the course catalog live in their own services and are fetched here via
 * StudentClient / CourseClient, each forwarding the caller's own bearer
 * token downstream (pass-through auth) so ownership rules apply
 * identically no matter which service the caller originally hit.
 *
 * Access rules enforced here (in addition to @PreAuthorize on the
 * controller for admin-only actions):
 * - A STUDENT may only enroll THEMSELVES (their token's studentId is used,
 *   overriding whatever studentId was sent in the request body) and may
 *   only view/drop their OWN enrollments.
 * - An ADMIN may enroll/view/drop on behalf of any student.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentClient studentClient;
    private final CourseClient courseClient;

    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO request, String bearerToken) {
        JwtPrincipal principal = requirePrincipal();

        // A STUDENT can only ever enroll themselves - the token's studentId
        // is authoritative, regardless of what the request body claims.
        Long effectiveStudentId = principal.isAdmin() ? request.getStudentId() : principal.getStudentId();

        CourseDTO course = courseClient.getCourseById(request.getCourseId(), bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        long confirmedCount = enrollmentRepository.countByCourseIdAndStatus(course.getId(), Enrollment.EnrollmentStatus.CONFIRMED);
        if (course.getCapacity() != null && confirmedCount >= course.getCapacity()) {
            throw new CourseCapacityExceededException(
                    "Course '" + course.getCourseCode() + "' has reached its capacity of " + course.getCapacity());
        }

        StudentDTO student = studentClient.getStudentById(effectiveStudentId, bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + effectiveStudentId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(effectiveStudentId, request.getCourseId())) {
            throw new DuplicateResourceException(
                    "Student " + effectiveStudentId + " is already enrolled in course " + request.getCourseId());
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(effectiveStudentId)
                .courseId(course.getId())
                .status(Enrollment.EnrollmentStatus.CONFIRMED)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        return EnrollmentResponseDTO.fromEntity(saved, student, course);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponseDTO getEnrollmentById(Long id, String bearerToken) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        requireAdminOrOwner(enrollment.getStudentId());
        StudentDTO student = studentClient.getStudentById(enrollment.getStudentId(), bearerToken).orElse(null);
        CourseDTO course = courseClient.getCourseById(enrollment.getCourseId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(enrollment, student, course);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByStudent(Long studentId, String bearerToken) {
        requireAdminOrOwner(studentId);
        StudentDTO student = studentClient.getStudentById(studentId, bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(e -> {
                    CourseDTO course = courseClient.getCourseById(e.getCourseId(), bearerToken).orElse(null);
                    return EnrollmentResponseDTO.fromEntity(e, student, course);
                })
                .toList();
    }

    /** ADMIN only - see EnrollmentController's @PreAuthorize. */
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDTO> getEnrollmentsByCourse(Long courseId, String bearerToken) {
        CourseDTO course = courseClient.getCourseById(courseId, bearerToken).orElse(null);
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> {
                    StudentDTO student = studentClient.getStudentById(e.getStudentId(), bearerToken).orElse(null);
                    return EnrollmentResponseDTO.fromEntity(e, student, course);
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
        CourseDTO course = courseClient.getCourseById(saved.getCourseId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student, course);
    }

    /**
     * @deprecated grade-service now owns grade assignment and GPA/CGPA
     * calculation. This is retained only for backward compatibility with
     * existing integrations against the original enrollment-service API.
     */
    @Deprecated
    public EnrollmentResponseDTO recordGrade(Long enrollmentId, Double grade, String bearerToken) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        enrollment.setGrade(grade);
        enrollment.setStatus(Enrollment.EnrollmentStatus.COMPLETED);
        Enrollment saved = enrollmentRepository.save(enrollment);
        StudentDTO student = studentClient.getStudentById(saved.getStudentId(), bearerToken).orElse(null);
        CourseDTO course = courseClient.getCourseById(saved.getCourseId(), bearerToken).orElse(null);
        return EnrollmentResponseDTO.fromEntity(saved, student, course);
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
