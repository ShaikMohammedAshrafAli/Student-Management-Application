package com.example.gradeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Overall CGPA plus a semester-wise GPA breakdown, both computed as a
 * credit-weighted average: sum(gradePoints * credits) / sum(credits).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpaResponseDTO {
    private Long studentId;
    private Double cgpa;
    private Integer totalCredits;
    private Map<String, Double> gpaBySemester;
}
