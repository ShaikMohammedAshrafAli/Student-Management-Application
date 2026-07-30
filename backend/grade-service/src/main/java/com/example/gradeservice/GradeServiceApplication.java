package com.example.gradeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Grade Service. Owns grade assignment and
 * GPA/CGPA calculation. An ADMIN assigns a grade against an existing
 * enrollment (verified via enrollment-service); credits are snapshotted
 * from course-service at assignment time so GPA math stays correct even
 * if a course's credit value changes later. A STUDENT can only view
 * their own grades and GPA.
 */
@SpringBootApplication
public class GradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradeServiceApplication.class, args);
    }
}
