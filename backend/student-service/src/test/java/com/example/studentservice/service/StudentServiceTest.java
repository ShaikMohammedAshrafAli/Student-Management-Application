package com.example.studentservice.service;

import com.example.studentservice.dto.StudentDTO;
import com.example.studentservice.entity.Student;
import com.example.studentservice.exception.DuplicateResourceException;
import com.example.studentservice.exception.ResourceNotFoundException;
import com.example.studentservice.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentService's business logic, with StudentRepository
 * mocked out - these run without a database or Spring context.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(studentRepository);
    }

    private Student sampleStudent() {
        return Student.builder()
                .id(1L)
                .firstName("Asha")
                .lastName("Rao")
                .email("asha.rao@example.com")
                .phoneNumber("9876543210")
                .dateOfBirth(LocalDate.of(2001, 5, 12))
                .status(Student.StudentStatus.ACTIVE)
                .build();
    }

    @Test
    void createStudent_savesAndReturnsTheStudent_whenEmailIsNotTaken() {
        StudentDTO request = StudentDTO.builder()
                .firstName("Asha")
                .lastName("Rao")
                .email("asha.rao@example.com")
                .build();

        when(studentRepository.existsByEmail("asha.rao@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent());

        StudentDTO result = studentService.createStudent(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("asha.rao@example.com");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void createStudent_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        StudentDTO request = StudentDTO.builder()
                .firstName("Asha")
                .lastName("Rao")
                .email("asha.rao@example.com")
                .build();

        when(studentRepository.existsByEmail("asha.rao@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("asha.rao@example.com");

        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_alwaysClearsAnyIncomingId_soClientsCannotChooseTheirOwnId() {
        StudentDTO request = StudentDTO.builder()
                .id(999L) // a caller should never be able to dictate the generated id
                .firstName("Asha")
                .lastName("Rao")
                .email("asha.rao@example.com")
                .build();

        when(studentRepository.existsByEmail(anyString())).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(sampleStudent());

        studentService.createStudent(request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void getStudentById_returnsTheStudent_whenItExists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent()));

        StudentDTO result = studentService.getStudentById(1L);

        assertThat(result.getFirstName()).isEqualTo("Asha");
    }

    @Test
    void getStudentById_throwsResourceNotFoundException_whenItDoesNotExist() {
        when(studentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void updateStudent_onlyOverwritesFieldsThatWerePresentInTheRequest() {
        Student existing = sampleStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only phoneNumber is being updated - everything else should be untouched.
        StudentDTO partialUpdate = StudentDTO.builder().phoneNumber("1112223333").build();

        StudentDTO result = studentService.updateStudent(1L, partialUpdate);

        assertThat(result.getPhoneNumber()).isEqualTo("1112223333");
        assertThat(result.getFirstName()).isEqualTo("Asha");
        assertThat(result.getEmail()).isEqualTo("asha.rao@example.com");
    }

    @Test
    void updateStudent_throwsDuplicateResourceException_whenNewEmailBelongsToSomeoneElse() {
        Student existing = sampleStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmail("taken@example.com")).thenReturn(true);

        StudentDTO update = StudentDTO.builder().email("taken@example.com").build();

        assertThatThrownBy(() -> studentService.updateStudent(1L, update))
                .isInstanceOf(DuplicateResourceException.class);

        verify(studentRepository, never()).save(any());
    }

    @Test
    void updateStudent_allowsKeepingTheSameEmailUnchanged_withoutTriggeringADuplicateCheck() {
        Student existing = sampleStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        // Same email, different casing - should be treated as "no change", not a duplicate.
        StudentDTO update = StudentDTO.builder().email("ASHA.RAO@EXAMPLE.COM").build();

        studentService.updateStudent(1L, update);

        verify(studentRepository, never()).existsByEmail(anyString());
    }

    @Test
    void deleteStudent_throwsResourceNotFoundException_whenTheStudentDoesNotExist() {
        when(studentRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.deleteStudent(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(studentRepository, never()).deleteById(any());
    }

    @Test
    void deleteStudent_deletesTheStudent_whenItExists() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        studentService.deleteStudent(1L);

        verify(studentRepository).deleteById(1L);
    }
}
