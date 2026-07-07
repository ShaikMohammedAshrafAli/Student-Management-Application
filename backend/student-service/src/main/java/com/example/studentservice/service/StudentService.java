package com.example.studentservice.service;

import com.example.studentservice.dto.StudentDTO;
import com.example.studentservice.entity.Student;
import com.example.studentservice.exception.DuplicateResourceException;
import com.example.studentservice.exception.ResourceNotFoundException;
import com.example.studentservice.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentDTO createStudent(StudentDTO dto) {
        if (studentRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("A student with email '" + dto.getEmail() + "' already exists");
        }
        Student student = dto.toEntity();
        student.setId(null);
        Student saved = studentRepository.save(student);
        return StudentDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return StudentDTO.fromEntity(student);
    }

    @Transactional(readOnly = true)
    public StudentDTO getStudentByEmail(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));
        return StudentDTO.fromEntity(student);
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(StudentDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentDTO> getStudents(Pageable pageable) {
        return studentRepository.findAll(pageable).map(StudentDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<StudentDTO> searchStudents(String keyword, Pageable pageable) {
        return studentRepository.search(keyword, pageable).map(StudentDTO::fromEntity);
    }

    public StudentDTO updateStudent(Long id, StudentDTO dto) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(existing.getEmail())
                && studentRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("A student with email '" + dto.getEmail() + "' already exists");
        }

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null) existing.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getDateOfBirth() != null) existing.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getStatus() != null) existing.setStatus(Student.StudentStatus.valueOf(dto.getStatus().toUpperCase()));

        Student updated = studentRepository.save(existing);
        return StudentDTO.fromEntity(updated);
    }

    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return studentRepository.existsById(id);
    }
}
