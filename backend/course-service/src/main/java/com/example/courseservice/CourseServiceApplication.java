package com.example.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Course Service. Owns the course catalog: capacity,
 * credits, semester, instructor, department, and status. Previously this
 * lived inside enrollment-service; it's split out here so course data has
 * its own bounded context and database (course_db), with
 * enrollment-service calling it over REST via CourseClient instead of
 * owning course rows directly.
 */
@SpringBootApplication
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
