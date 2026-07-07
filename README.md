# Student Management Microservices

A multi-service backend built with **Java 17 + Spring Boot 3**, modeling a student
data and course-enrollment lifecycle workflow across two independently deployable
services that communicate over REST.

## Architecture

```
                        ┌─────────────────────┐
                        │   Postman / Client  │
                        └──────────┬──────────┘
                                   │
                  ┌────────────────┴────────────────┐
                  │                                 │
          ┌───────▼────────┐               ┌────────▼─────────┐
          │ student-service │               │ enrollment-service│
          │   (port 8081)   │◄──────REST────┤   (port 8082)     │
          │ Controller      │  GET /students │ Controller        │
          │ → Service       │  /{id}/exists  │ → Service          │
          │ → Repository    │                │ → Repository        │
          └───────┬─────────┘                └─────────┬───────────┘
                  │                                     │
           ┌──────▼──────┐                       ┌──────▼───────┐
           │ MySQL        │                       │ MySQL         │
           │ student_db   │                       │ enrollment_db │
           └──────────────┘                       └───────────────┘
```

- **student-service** owns the `Student` domain: registration, profile lookups,
  and a lightweight `/exists` endpoint for other services to validate a student
  without pulling the full profile.
- **enrollment-service** owns `Course` and `Enrollment` domains. Before creating
  an enrollment it calls student-service (via a reactive `WebClient`) to confirm
  the student exists, enforces course capacity, and prevents duplicate
  enrollments — a small workflow-automation pipeline around the enrollment
  lifecycle (`PENDING → CONFIRMED → COMPLETED/DROPPED/REJECTED`).
- Each service follows a strict **Controller → Service → Repository** layering,
  uses **Spring Data JPA** for persistence, and returns a consistent JSON error
  shape via a `@RestControllerAdvice` global exception handler.

## Tech Stack

| Concern              | Choice                              |
|-----------------------|--------------------------------------|
| Language / Runtime    | Java 17                              |
| Framework             | Spring Boot 3.3.x                    |
| Persistence           | Spring Data JPA + MySQL 8            |
| Inter-service calls   | Spring WebFlux `WebClient`           |
| Validation            | Jakarta Bean Validation              |
| Build                 | Maven                                |
| Containerization      | Docker + Docker Compose              |
| API testing           | Postman collection (included)        |

## Project Layout

```
student-management-microservices/
├── student-service/
│   ├── src/main/java/com/example/studentservice/
│   │   ├── controller/StudentController.java
│   │   ├── service/StudentService.java
│   │   ├── repository/StudentRepository.java
│   │   ├── entity/Student.java
│   │   ├── dto/StudentDTO.java
│   │   └── exception/ (custom exceptions + global handler)
│   ├── src/main/resources/application.properties
│   ├── pom.xml
│   └── Dockerfile
├── enrollment-service/
│   ├── src/main/java/com/example/enrollmentservice/
│   │   ├── controller/{CourseController, EnrollmentController}.java
│   │   ├── service/{CourseService, EnrollmentService}.java
│   │   ├── repository/{CourseRepository, EnrollmentRepository}.java
│   │   ├── entity/{Course, Enrollment}.java
│   │   ├── dto/{CourseDTO, EnrollmentRequestDTO, EnrollmentResponseDTO, StudentDTO}.java
│   │   ├── client/StudentClient.java      ← inter-service REST client
│   │   ├── config/WebClientConfig.java
│   │   └── exception/ (custom exceptions + global handler)
│   ├── src/main/resources/application.properties
│   ├── pom.xml
│   └── Dockerfile
├── postman/Student-Management-Microservices.postman_collection.json
├── docker-compose.yml
└── README.md
```

## Running Locally

### Option A — Docker Compose (recommended)

```bash
cd student-management-microservices
docker compose up --build
```

This spins up two MySQL instances and both services:

| Service            | URL                          |
|--------------------|-------------------------------|
| student-service    | http://localhost:8081/api/v1  |
| enrollment-service  | http://localhost:8082/api/v1  |

### Option B — Run each service manually

1. Start a local MySQL instance and create `student_db` and `enrollment_db`
   (or let `createDatabaseIfNotExist=true` in the JDBC URL handle it).
2. In one terminal:
   ```bash
   cd student-service
   mvn spring-boot:run
   ```
3. In another terminal:
   ```bash
   cd enrollment-service
   mvn spring-boot:run
   ```
   Set `STUDENT_SERVICE_URL=http://localhost:8081` if student-service isn't on
   its default port.

## Example Workflow

```bash
# 1. Create a student
curl -X POST http://localhost:8081/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Asha","lastName":"Rao","email":"asha.rao@example.com","dateOfBirth":"2001-05-12"}'

# 2. Create a course
curl -X POST http://localhost:8082/api/v1/courses \
  -H "Content-Type: application/json" \
  -d '{"courseCode":"CS101","title":"Intro to CS","credits":4,"capacity":30}'

# 3. Enroll the student (enrollment-service validates the student via REST call to student-service)
curl -X POST http://localhost:8082/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"courseId":1}'

# 4. View a student's enrollments
curl http://localhost:8082/api/v1/enrollments/student/1
```

## API Reference

### student-service (`/api/v1/students`)
| Method | Path                    | Description                          |
|--------|-------------------------|----------------------------------------|
| POST   | `/students`             | Create a student                       |
| GET    | `/students/{id}`        | Get student by id                       |
| GET    | `/students/email/{email}` | Get student by email                 |
| GET    | `/students?keyword=&page=&size=` | Paginated search                |
| GET    | `/students?unpaged=true`| Get all students                        |
| PUT    | `/students/{id}`        | Update a student                        |
| DELETE | `/students/{id}`        | Delete a student                        |
| GET    | `/students/{id}/exists` | Existence check (used by enrollment-service) |

### enrollment-service (`/api/v1/courses`, `/api/v1/enrollments`)
| Method | Path                                | Description                          |
|--------|--------------------------------------|----------------------------------------|
| POST   | `/courses`                          | Create a course                        |
| GET    | `/courses` / `/courses/{id}`        | List / get courses                     |
| PUT    | `/courses/{id}`                     | Update a course                        |
| DELETE | `/courses/{id}`                     | Delete a course                        |
| POST   | `/enrollments`                      | Enroll a student (validates against student-service, checks capacity & duplicates) |
| GET    | `/enrollments/{id}`                 | Get one enrollment                      |
| GET    | `/enrollments/student/{studentId}`  | All enrollments for a student           |
| GET    | `/enrollments/course/{courseId}`    | All enrollments for a course            |
| PATCH  | `/enrollments/{id}/status`          | Update lifecycle status                 |
| PATCH  | `/enrollments/{id}/grade`           | Record final grade (marks COMPLETED)    |
| DELETE | `/enrollments/{id}`                 | Drop an enrollment                      |

## Postman

Import `postman/Student-Management-Microservices.postman_collection.json` into
Postman. It's organized into a `Student Service` and `Enrollment Service`
folder with ready-to-run requests, including the enrollment call that
exercises the inter-service REST path end-to-end.

## Notes on Production-Readiness

- Global exception handlers return a consistent JSON error body
  (`timestamp`, `status`, `error`, `message`, `path`, `validationErrors`).
- `StudentClient` wraps all outbound HTTP calls with a timeout and translates
  connectivity failures into a `503 Service Unavailable` rather than leaking
  a raw connection exception.
- Indexes are defined on `email` (student-service) and `courseCode` /
  `(studentId, course_id)` (enrollment-service) to keep CRUD-heavy queries
  fast.
- Each service has its own database (`student_db`, `enrollment_db`) — a
  database-per-service pattern, so the two remain independently deployable.
