package com.example.studentservice.controller;

import com.example.common.security.JwtPrincipal;
import com.example.common.security.SecurityUtils;
import com.example.studentservice.dto.StudentDTO;
import com.example.studentservice.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for student CRUD operations.
 * Base path: /api/v1/students
 *
 * Access rules:
 * - ADMIN can do everything.
 * - STUDENT can only read/update their OWN profile (matched via the
 *   studentId claim embedded in their JWT by auth-service), and cannot
 *   list/search all students, create, or delete any profile.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO created = studentService.createStudent(studentDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        requireAdminOrOwner(id);
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDTO> getStudentByEmail(@PathVariable String email) {
        return ResponseEntity.ok(studentService.getStudentByEmail(email));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "false") boolean unpaged) {

        if (unpaged) {
            List<StudentDTO> all = studentService.getAllStudents();
            return ResponseEntity.ok(all);
        }

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by(sortBy));
        Page<StudentDTO> result = (keyword != null && !keyword.isBlank())
                ? studentService.searchStudents(keyword, pageable)
                : studentService.getStudents(pageable);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @RequestBody StudentDTO studentDTO) {
        requireAdminOrOwner(id);
        return ResponseEntity.ok(studentService.updateStudent(id, studentDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lightweight existence check used by enrollment-service before
     * creating an enrollment. Any authenticated caller may use this -
     * it leaks no profile data, only a boolean.
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.existsById(id));
    }

    private void requireAdminOrOwner(Long studentId) {
        JwtPrincipal principal = SecurityUtils.currentUser();
        if (principal == null || (!principal.isAdmin() && !principal.ownsStudentId(studentId))) {
            throw new AccessDeniedException("You may only access your own student record");
        }
    }
}
