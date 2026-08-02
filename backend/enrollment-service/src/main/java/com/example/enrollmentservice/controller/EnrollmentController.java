package com.example.enrollmentservice.controller;

import com.example.enrollmentservice.dto.EnrollmentRequestDTO;
import com.example.enrollmentservice.dto.EnrollmentResponseDTO;
import com.example.enrollmentservice.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Access rules:
 * - POST /enrollments: any authenticated user; a STUDENT can only enroll
 *   themselves (enforced in EnrollmentService using the JWT's studentId,
 *   regardless of the body), an ADMIN can enroll anyone.
 * - GET by student / by id: ADMIN or the owning STUDENT only.
 * - GET by course, PATCH status/grade: ADMIN only.
 * - DELETE (drop): ADMIN or the owning STUDENT only.
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Student <-> course enrollment lifecycle")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(summary = "Enroll a student in a course (validates against student-service and course-service)")
    public ResponseEntity<EnrollmentResponseDTO> enroll(@Valid @RequestBody EnrollmentRequestDTO request,
                                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return new ResponseEntity<>(enrollmentService.enrollStudent(request, authHeader), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single enrollment (ADMIN or the owning student)")
    public ResponseEntity<EnrollmentResponseDTO> getEnrollment(@PathVariable Long id,
                                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id, authHeader));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "List all enrollments for a student (ADMIN or the owning student)")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByStudent(@PathVariable Long studentId,
                                                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId, authHeader));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all enrollments for a course - i.e. its roster (ADMIN only)")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByCourse(@PathVariable Long courseId,
                                                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId, authHeader));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an enrollment's lifecycle status (ADMIN only)")
    public ResponseEntity<EnrollmentResponseDTO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body,
                                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.updateStatus(id, body.get("status"), authHeader));
    }

    /**
     * @deprecated grade-service is now the authoritative source for grades
     * and GPA/CGPA calculation (see POST /api/v1/grades). This endpoint is
     * retained only for backward compatibility.
     */
    @Deprecated
    @PatchMapping("/{id}/grade")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "[Deprecated] Record a grade directly on the enrollment",
            description = "Superseded by grade-service's POST /api/v1/grades, which links a grade to a " +
                    "verified enrollment and computes GPA/CGPA. Kept only for backward compatibility."
    )
    public ResponseEntity<EnrollmentResponseDTO> recordGrade(@PathVariable Long id, @RequestBody Map<String, Double> body,
                                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.recordGrade(id, body.get("grade"), authHeader));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Drop an enrollment (ADMIN or the owning student)")
    public ResponseEntity<Void> drop(@PathVariable Long id) {
        enrollmentService.dropEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}
