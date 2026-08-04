package com.example.gradeservice.service;

import com.example.common.security.JwtPrincipal;
import com.example.gradeservice.client.CourseClient;
import com.example.gradeservice.client.EnrollmentClient;
import com.example.gradeservice.dto.*;
import com.example.gradeservice.entity.Grade;
import com.example.gradeservice.exception.InvalidEnrollmentException;
import com.example.gradeservice.exception.ResourceNotFoundException;
import com.example.gradeservice.repository.GradeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GradeService: the enrollment-gated grade assignment
 * rule (a grade can never exist without a real, gradable enrollment) and
 * the GPA/CGPA credit-weighted-average calculation, which is pure enough
 * logic to be worth testing thoroughly on its own.
 */
@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock private GradeRepository gradeRepository;
    @Mock private EnrollmentClient enrollmentClient;
    @Mock private CourseClient courseClient;

    private GradeService gradeService;

    private static final String FAKE_TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(gradeRepository, enrollmentClient, courseClient);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(JwtPrincipal principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private EnrollmentDTO sampleEnrollment(String status) {
        return EnrollmentDTO.builder().id(100L).studentId(1L).courseId(10L).status(status).build();
    }

    private CourseDTO sampleCourse(int credits, String semester) {
        return CourseDTO.builder().id(10L).courseCode("CS101").title("Intro to CS").credits(credits).semester(semester).build();
    }

    // ---------- assignGrade ----------

    @Test
    void assignGrade_throwsInvalidEnrollmentException_whenEnrollmentIsPending() {
        GradeAssignRequestDTO request = GradeAssignRequestDTO.builder().enrollmentId(100L).gradePoints(8.5).build();

        when(enrollmentClient.getEnrollmentById(100L, FAKE_TOKEN)).thenReturn(Optional.of(sampleEnrollment("PENDING")));

        assertThatThrownBy(() -> gradeService.assignGrade(request, FAKE_TOKEN))
                .isInstanceOf(InvalidEnrollmentException.class)
                .hasMessageContaining("PENDING");

        verifyNoInteractions(courseClient, gradeRepository);
    }

    @Test
    void assignGrade_throwsInvalidEnrollmentException_whenEnrollmentWasDropped() {
        GradeAssignRequestDTO request = GradeAssignRequestDTO.builder().enrollmentId(100L).gradePoints(8.5).build();

        when(enrollmentClient.getEnrollmentById(100L, FAKE_TOKEN)).thenReturn(Optional.of(sampleEnrollment("DROPPED")));

        assertThatThrownBy(() -> gradeService.assignGrade(request, FAKE_TOKEN))
                .isInstanceOf(InvalidEnrollmentException.class);
    }

    @Test
    void assignGrade_throwsResourceNotFoundException_whenTheEnrollmentDoesNotExist() {
        GradeAssignRequestDTO request = GradeAssignRequestDTO.builder().enrollmentId(999L).gradePoints(8.5).build();

        when(enrollmentClient.getEnrollmentById(999L, FAKE_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.assignGrade(request, FAKE_TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignGrade_createsANewGrade_snapshottingCreditsAndSemesterFromCourseService() {
        GradeAssignRequestDTO request = GradeAssignRequestDTO.builder().enrollmentId(100L).gradePoints(8.5).build();

        when(enrollmentClient.getEnrollmentById(100L, FAKE_TOKEN)).thenReturn(Optional.of(sampleEnrollment("CONFIRMED")));
        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(4, "FALL2026")));
        when(gradeRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeResponseDTO result = gradeService.assignGrade(request, FAKE_TOKEN);

        assertThat(result.getGradePoints()).isEqualTo(8.5);
        assertThat(result.getCredits()).isEqualTo(4);
        assertThat(result.getSemester()).isEqualTo("FALL2026");
        assertThat(result.getStudentId()).isEqualTo(1L);
    }

    @Test
    void assignGrade_updatesTheExistingGrade_insteadOfCreatingADuplicate_whenOneAlreadyExists() {
        GradeAssignRequestDTO request = GradeAssignRequestDTO.builder().enrollmentId(100L).gradePoints(9.0).build();

        Grade existingGrade = Grade.builder().id(55L).studentId(1L).courseId(10L).enrollmentId(100L)
                .credits(4).semester("FALL2026").gradePoints(6.0).build();

        when(enrollmentClient.getEnrollmentById(100L, FAKE_TOKEN)).thenReturn(Optional.of(sampleEnrollment("COMPLETED")));
        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(4, "FALL2026")));
        when(gradeRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(existingGrade));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));

        gradeService.assignGrade(request, FAKE_TOKEN);

        ArgumentCaptor<Grade> captor = ArgumentCaptor.forClass(Grade.class);
        verify(gradeRepository).save(captor.capture());
        // Same row (id 55) updated in place, not a new one.
        assertThat(captor.getValue().getId()).isEqualTo(55L);
        assertThat(captor.getValue().getGradePoints()).isEqualTo(9.0);
    }

    // ---------- ownership ----------

    @Test
    void getGradesByStudent_throwsAccessDenied_whenAStudentRequestsSomeoneElsesGrades() {
        authenticateAs(JwtPrincipal.builder().role("STUDENT").studentId(1L).build());

        assertThatThrownBy(() -> gradeService.getGradesByStudent(2L, FAKE_TOKEN))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(gradeRepository);
    }

    // ---------- GPA / CGPA calculation ----------

    @Test
    void getGpa_computesACreditWeightedAverage_acrossMultipleCourses() {
        authenticateAs(JwtPrincipal.builder().role("STUDENT").studentId(1L).build());

        // (8.0 * 4 + 6.0 * 3) / (4 + 3) = 50 / 7 = 7.1428... -> rounds to 7.14
        List<Grade> grades = List.of(
                Grade.builder().studentId(1L).courseId(10L).credits(4).gradePoints(8.0).semester("FALL2026").build(),
                Grade.builder().studentId(1L).courseId(11L).credits(3).gradePoints(6.0).semester("FALL2026").build()
        );
        when(gradeRepository.findByStudentId(1L)).thenReturn(grades);

        GpaResponseDTO result = gradeService.getGpa(1L);

        assertThat(result.getCgpa()).isEqualTo(7.14);
        assertThat(result.getTotalCredits()).isEqualTo(7);
    }

    @Test
    void getGpa_breaksDownGpaPerSemester_separatelyFromTheOverallCgpa() {
        authenticateAs(JwtPrincipal.builder().role("STUDENT").studentId(1L).build());

        List<Grade> grades = List.of(
                Grade.builder().studentId(1L).courseId(10L).credits(4).gradePoints(8.0).semester("FALL2025").build(),
                Grade.builder().studentId(1L).courseId(11L).credits(4).gradePoints(6.0).semester("SPRING2026").build()
        );
        when(gradeRepository.findByStudentId(1L)).thenReturn(grades);

        GpaResponseDTO result = gradeService.getGpa(1L);

        assertThat(result.getGpaBySemester())
                .containsEntry("FALL2025", 8.0)
                .containsEntry("SPRING2026", 6.0);
        // Overall CGPA blends both semesters evenly here (equal credits).
        assertThat(result.getCgpa()).isEqualTo(7.0);
    }

    @Test
    void getGpa_returnsZero_whenTheStudentHasNoGradesYet() {
        authenticateAs(JwtPrincipal.builder().role("STUDENT").studentId(1L).build());
        when(gradeRepository.findByStudentId(1L)).thenReturn(List.of());

        GpaResponseDTO result = gradeService.getGpa(1L);

        assertThat(result.getCgpa()).isEqualTo(0.0);
        assertThat(result.getTotalCredits()).isEqualTo(0);
        assertThat(result.getGpaBySemester()).isEmpty();
    }

    @Test
    void getGpa_allowsAnAdminToViewAnyStudentsGpa() {
        authenticateAs(JwtPrincipal.builder().role("ADMIN").build());
        when(gradeRepository.findByStudentId(2L)).thenReturn(List.of());

        assertThat(gradeService.getGpa(2L).getStudentId()).isEqualTo(2L);
    }
}
