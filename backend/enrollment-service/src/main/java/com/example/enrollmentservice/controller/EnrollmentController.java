package com.example.enrollmentservice.controller;

import com.example.enrollmentservice.dto.EnrollmentRequestDTO;
import com.example.enrollmentservice.dto.EnrollmentResponseDTO;
import com.example.enrollmentservice.service.EnrollmentService;
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
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponseDTO> enroll(@Valid @RequestBody EnrollmentRequestDTO request,
                                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return new ResponseEntity<>(enrollmentService.enrollStudent(request, authHeader), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponseDTO> getEnrollment(@PathVariable Long id,
                                                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id, authHeader));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByStudent(@PathVariable Long studentId,
                                                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId, authHeader));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentResponseDTO>> getByCourse(@PathVariable Long courseId,
                                                                    @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId, authHeader));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponseDTO> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body,
                                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.updateStatus(id, body.get("status"), authHeader));
    }

    @PatchMapping("/{id}/grade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponseDTO> recordGrade(@PathVariable Long id, @RequestBody Map<String, Double> body,
                                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(enrollmentService.recordGrade(id, body.get("grade"), authHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> drop(@PathVariable Long id) {
        enrollmentService.dropEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}
