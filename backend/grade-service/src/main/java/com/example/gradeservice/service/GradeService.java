package com.example.gradeservice.service;

import com.example.common.security.JwtPrincipal;
import com.example.common.security.SecurityUtils;
import com.example.gradeservice.client.CourseClient;
import com.example.gradeservice.client.EnrollmentClient;
import com.example.gradeservice.dto.*;
import com.example.gradeservice.entity.Grade;
import com.example.gradeservice.exception.InvalidEnrollmentException;
import com.example.gradeservice.exception.ResourceNotFoundException;
import com.example.gradeservice.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Access rules:
 * - Only ADMIN can assign a grade (enforced via @PreAuthorize on the
 *   controller).
 * - A STUDENT can only view their OWN grades and GPA; an ADMIN can view
 *   anyone's.
 *
 * Grades are assigned against a specific enrollmentId rather than a raw
 * (studentId, courseId) pair, so a grade can never exist without a real,
 * verified enrollment behind it. credits/semester are pulled from
 * course-service and snapshotted onto the Grade row at assignment time.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GradeService {

    private static final Set<String> GRADABLE_STATUSES = Set.of("CONFIRMED", "COMPLETED");

    private final GradeRepository gradeRepository;
    private final EnrollmentClient enrollmentClient;
    private final CourseClient courseClient;

    public GradeResponseDTO assignGrade(GradeAssignRequestDTO request, String bearerToken) {
        EnrollmentDTO enrollment = enrollmentClient.getEnrollmentById(request.getEnrollmentId(), bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + request.getEnrollmentId()));

        if (!GRADABLE_STATUSES.contains(enrollment.getStatus())) {
            throw new InvalidEnrollmentException(
                    "Enrollment " + enrollment.getId() + " is " + enrollment.getStatus() +
                            " and cannot be graded (must be CONFIRMED or COMPLETED)");
        }

        CourseDTO course = courseClient.getCourseById(enrollment.getCourseId(), bearerToken)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + enrollment.getCourseId()));

        // Upsert: re-assigning a grade for the same student+course updates the existing row.
        Grade grade = gradeRepository.findByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId())
                .map(existing -> {
                    existing.setGradePoints(request.getGradePoints());
                    existing.setCredits(course.getCredits());
                    existing.setSemester(course.getSemester());
                    existing.setEnrollmentId(enrollment.getId());
                    return existing;
                })
                .orElseGet(() -> Grade.builder()
                        .studentId(enrollment.getStudentId())
                        .courseId(enrollment.getCourseId())
                        .enrollmentId(enrollment.getId())
                        .semester(course.getSemester())
                        .credits(course.getCredits())
                        .gradePoints(request.getGradePoints())
                        .build());

        Grade saved = gradeRepository.save(grade);
        return GradeResponseDTO.fromEntity(saved, course);
    }

    @Transactional(readOnly = true)
    public List<GradeResponseDTO> getGradesByStudent(Long studentId, String bearerToken) {
        requireAdminOrOwner(studentId);
        return gradeRepository.findByStudentId(studentId).stream()
                .map(grade -> {
                    CourseDTO course = courseClient.getCourseById(grade.getCourseId(), bearerToken).orElse(null);
                    return GradeResponseDTO.fromEntity(grade, course);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GpaResponseDTO getGpa(Long studentId) {
        requireAdminOrOwner(studentId);
        List<Grade> grades = gradeRepository.findByStudentId(studentId);

        Map<String, Double> gpaBySemester = new LinkedHashMap<>();
        Map<String, Double> semesterWeightedSum = new LinkedHashMap<>();
        Map<String, Integer> semesterCredits = new LinkedHashMap<>();

        double overallWeightedSum = 0.0;
        int overallCredits = 0;

        for (Grade grade : grades) {
            String semester = grade.getSemester() != null ? grade.getSemester() : "UNSPECIFIED";
            int credits = grade.getCredits() != null ? grade.getCredits() : 0;
            double points = grade.getGradePoints() != null ? grade.getGradePoints() : 0.0;

            overallWeightedSum += points * credits;
            overallCredits += credits;

            semesterWeightedSum.merge(semester, points * credits, Double::sum);
            semesterCredits.merge(semester, credits, Integer::sum);
        }

        for (String semester : semesterWeightedSum.keySet()) {
            int credits = semesterCredits.get(semester);
            double weighted = semesterWeightedSum.get(semester);
            gpaBySemester.put(semester, credits > 0 ? round(weighted / credits) : 0.0);
        }

        double cgpa = overallCredits > 0 ? round(overallWeightedSum / overallCredits) : 0.0;

        return GpaResponseDTO.builder()
                .studentId(studentId)
                .cgpa(cgpa)
                .totalCredits(overallCredits)
                .gpaBySemester(gpaBySemester)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void requireAdminOrOwner(Long studentId) {
        JwtPrincipal principal = SecurityUtils.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!principal.isAdmin() && !principal.ownsStudentId(studentId)) {
            throw new AccessDeniedException("You may only access your own grades");
        }
    }
}
