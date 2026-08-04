package com.example.enrollmentservice.service;

import com.example.common.security.JwtPrincipal;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrollmentService, the most business-rule-heavy service
 * in the system: capacity checks, duplicate-enrollment prevention, and
 * the "a STUDENT can only ever enroll themselves" ownership rule. All
 * three collaborators (repository, StudentClient, CourseClient) are
 * mocked, so these run without a database or a running student-service /
 * course-service.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentClient studentClient;
    @Mock private CourseClient courseClient;

    private EnrollmentService enrollmentService;

    private static final String FAKE_TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, studentClient, courseClient);
    }

    @AfterEach
    void clearSecurityContext() {
        // Every test that authenticates "as" a student/admin must clean up
        // afterwards so authentication never leaks between test methods.
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(JwtPrincipal principal) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private CourseDTO sampleCourse(int capacity) {
        return CourseDTO.builder().id(10L).courseCode("CS101").title("Intro to CS").credits(4).capacity(capacity).build();
    }

    private StudentDTO sampleStudent(Long id) {
        return StudentDTO.builder().id(id).firstName("Asha").lastName("Rao").email("asha@example.com").build();
    }

    // ---------- enrollStudent ----------

    @Test
    void enrollStudent_asAStudent_alwaysUsesTheirOwnTokenStudentId_ignoringTheRequestBody() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        // The request body claims studentId 999 - a student trying to enroll someone else.
        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(999L).courseId(10L).build();

        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(30)));
        when(enrollmentRepository.countByCourseIdAndStatus(10L, Enrollment.EnrollmentStatus.CONFIRMED)).thenReturn(5L);
        when(studentClient.getStudentById(1L, FAKE_TOKEN)).thenReturn(Optional.of(sampleStudent(1L)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponseDTO result = enrollmentService.enrollStudent(request, FAKE_TOKEN);

        // The enrollment was created for the TOKEN's studentId (1), not the body's (999).
        assertThat(result.getStudentId()).isEqualTo(1L);
        verify(studentClient).getStudentById(1L, FAKE_TOKEN);
        verify(studentClient, never()).getStudentById(999L, FAKE_TOKEN);
    }

    @Test
    void enrollStudent_asAnAdmin_respectsTheStudentIdFromTheRequestBody() {
        JwtPrincipal admin = JwtPrincipal.builder().role("ADMIN").build();
        authenticateAs(admin);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(5L).courseId(10L).build();

        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(30)));
        when(enrollmentRepository.countByCourseIdAndStatus(10L, Enrollment.EnrollmentStatus.CONFIRMED)).thenReturn(0L);
        when(studentClient.getStudentById(5L, FAKE_TOKEN)).thenReturn(Optional.of(sampleStudent(5L)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(5L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponseDTO result = enrollmentService.enrollStudent(request, FAKE_TOKEN);

        assertThat(result.getStudentId()).isEqualTo(5L);
    }

    @Test
    void enrollStudent_throwsCourseCapacityExceeded_whenConfirmedCountAlreadyMeetsCapacity() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(1L).courseId(10L).build();

        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(30)));
        when(enrollmentRepository.countByCourseIdAndStatus(10L, Enrollment.EnrollmentStatus.CONFIRMED)).thenReturn(30L);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(request, FAKE_TOKEN))
                .isInstanceOf(CourseCapacityExceededException.class)
                .hasMessageContaining("CS101");

        // Should fail fast on capacity, before ever calling student-service.
        verify(studentClient, never()).getStudentById(anyLong(), anyString());
    }

    @Test
    void enrollStudent_throwsDuplicateResourceException_whenAlreadyEnrolled() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(1L).courseId(10L).build();

        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(30)));
        when(enrollmentRepository.countByCourseIdAndStatus(10L, Enrollment.EnrollmentStatus.CONFIRMED)).thenReturn(5L);
        when(studentClient.getStudentById(1L, FAKE_TOKEN)).thenReturn(Optional.of(sampleStudent(1L)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(request, FAKE_TOKEN))
                .isInstanceOf(DuplicateResourceException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollStudent_throwsResourceNotFoundException_whenCourseDoesNotExist() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(1L).courseId(999L).build();

        when(courseClient.getCourseById(999L, FAKE_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enrollStudent(request, FAKE_TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enrollStudent_throwsResourceNotFoundException_whenStudentDoesNotExistInStudentService() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(1L).courseId(10L).build();

        when(courseClient.getCourseById(10L, FAKE_TOKEN)).thenReturn(Optional.of(sampleCourse(30)));
        when(enrollmentRepository.countByCourseIdAndStatus(10L, Enrollment.EnrollmentStatus.CONFIRMED)).thenReturn(5L);
        when(studentClient.getStudentById(1L, FAKE_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enrollStudent(request, FAKE_TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- ownership checks ----------

    @Test
    void getEnrollmentsByStudent_throwsAccessDenied_whenAStudentRequestsSomeoneElsesRecords() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        assertThatThrownBy(() -> enrollmentService.getEnrollmentsByStudent(2L, FAKE_TOKEN))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(studentClient, enrollmentRepository);
    }

    @Test
    void getEnrollmentsByStudent_succeeds_whenAStudentRequestsTheirOwnRecords() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        when(studentClient.getStudentById(1L, FAKE_TOKEN)).thenReturn(Optional.of(sampleStudent(1L)));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of());

        List<EnrollmentResponseDTO> result = enrollmentService.getEnrollmentsByStudent(1L, FAKE_TOKEN);

        assertThat(result).isEmpty();
    }

    @Test
    void getEnrollmentsByStudent_succeeds_whenAnAdminRequestsAnyStudentsRecords() {
        JwtPrincipal admin = JwtPrincipal.builder().role("ADMIN").build();
        authenticateAs(admin);

        when(studentClient.getStudentById(2L, FAKE_TOKEN)).thenReturn(Optional.of(sampleStudent(2L)));
        when(enrollmentRepository.findByStudentId(2L)).thenReturn(List.of());

        assertThat(enrollmentService.getEnrollmentsByStudent(2L, FAKE_TOKEN)).isEmpty();
    }

    @Test
    void dropEnrollment_throwsAccessDenied_whenAStudentTriesToDropSomeoneElsesEnrollment() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        Enrollment someoneElsesEnrollment = Enrollment.builder().id(100L).studentId(2L).courseId(10L)
                .status(Enrollment.EnrollmentStatus.CONFIRMED).build();
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(someoneElsesEnrollment));

        assertThatThrownBy(() -> enrollmentService.dropEnrollment(100L))
                .isInstanceOf(AccessDeniedException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void dropEnrollment_setsStatusToDropped_whenTheOwningStudentDropsIt() {
        JwtPrincipal student = JwtPrincipal.builder().role("STUDENT").studentId(1L).build();
        authenticateAs(student);

        Enrollment ownEnrollment = Enrollment.builder().id(100L).studentId(1L).courseId(10L)
                .status(Enrollment.EnrollmentStatus.CONFIRMED).build();
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(ownEnrollment));

        enrollmentService.dropEnrollment(100L);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Enrollment.EnrollmentStatus.DROPPED);
    }

    @Test
    void anyOperation_throwsAccessDenied_whenThereIsNoAuthenticatedPrincipalAtAll() {
        // No authenticateAs(...) call - simulates a gap in the security filter chain.
        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().studentId(1L).courseId(10L).build();

        assertThatThrownBy(() -> enrollmentService.enrollStudent(request, FAKE_TOKEN))
                .isInstanceOf(AccessDeniedException.class);
    }
}
