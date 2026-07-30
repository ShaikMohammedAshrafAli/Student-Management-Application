package com.example.gradeservice.controller;

import com.example.gradeservice.dto.GradeAssignRequestDTO;
import com.example.gradeservice.dto.GradeResponseDTO;
import com.example.gradeservice.dto.GpaResponseDTO;
import com.example.gradeservice.service.GradeService;
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

/**
 * Access rules:
 * - POST /grades: ADMIN only.
 * - GET by student, GET GPA: ADMIN or the owning STUDENT only (enforced
 *   in GradeService).
 */
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Grade assignment, semester-wise grades, and GPA/CGPA calculation")
public class GradeController {

    private final GradeService gradeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign (or update) a grade for an existing enrollment - ADMIN only")
    public ResponseEntity<GradeResponseDTO> assignGrade(@Valid @RequestBody GradeAssignRequestDTO request,
                                                         @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return new ResponseEntity<>(gradeService.assignGrade(request, authHeader), HttpStatus.CREATED);
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "List all grades for a student (ADMIN or the owning student)")
    public ResponseEntity<List<GradeResponseDTO>> getGradesByStudent(@PathVariable Long studentId,
                                                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(gradeService.getGradesByStudent(studentId, authHeader));
    }

    @GetMapping("/student/{studentId}/gpa")
    @Operation(summary = "CGPA plus semester-wise GPA breakdown (ADMIN or the owning student)")
    public ResponseEntity<GpaResponseDTO> getGpa(@PathVariable Long studentId) {
        return ResponseEntity.ok(gradeService.getGpa(studentId));
    }
}
