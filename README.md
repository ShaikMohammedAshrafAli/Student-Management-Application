# StudentHub

A full-stack, microservices-based Student Management System built with
**Java 17 / Spring Boot 3** on the backend and **React 19 / Vite / Material UI**
on the frontend — covering authentication & role-based access control,
student records, a course catalog, enrollment workflows, and GPA/CGPA
calculation, all fronted by a single API gateway.

This was built incrementally in phases, each one fully working end-to-end
before the next was layered on, which is why the design favors clear
service boundaries and independently-owned databases over shortcuts.

---

## Architecture (HLD)

![Architecture Diagram](docs/architecture-diagram.svg)

Six Spring Boot services, each with its own MySQL database (database-per-service),
sit behind a single Spring Cloud Gateway entry point. Services call each
other over plain REST — never by sharing a database — and every
inter-service call forwards the *original caller's own JWT* downstream
("pass-through auth"), so a service's ownership rules apply identically
whether it's called directly or through another service.

## Class Diagram (LLD)
![Class Diagram](docs/class-diagram.svg)



| Service              | Port | Responsibility                                      | Database       |
|-----------------------|------|--------------------------------------------------------|-----------------|
| `api-gateway`         | 9000 | Single entry point: routing, coarse JWT check, CORS    | —               |
| `auth-service`        | 8080 | Registration, login, JWT issuance/refresh, roles, users| `auth_db`       |
| `student-service`     | 8081 | Student profile CRUD, search, pagination                | `student_db`    |
| `course-service`      | 8083 | Course catalog: capacity, credits, semester, instructor | `course_db`     |
| `enrollment-service`  | 8082 | Enrollment lifecycle, capacity checks, duplicate prevention | `enrollment_db` |
| `grade-service`       | 8084 | Grade assignment (against a verified enrollment) + GPA/CGPA | `grade_db`      |

Every service **independently re-validates** the JWT it receives — the
gateway's check is defense in depth, not the sole security boundary. If a
request reached `student-service` directly (bypassing the gateway), it
would still be fully protected.

---

## Tech Stack

**Backend**
- Java 17, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA
- Spring Cloud Gateway (reactive) for the API gateway
- JWT (JJWT) for stateless access tokens + persisted, revocable refresh tokens
- BCrypt password hashing
- MySQL 8 (one schema per service)
- springdoc-openapi (Swagger UI) on every REST-exposing service
- Maven, Docker, Docker Compose
- Lombok, a shared `common-lib` module (DTOs, exceptions, JWT validation)

**Frontend**
- React 19, Vite 8
- Material UI (MUI) v9
- react-router-dom v7 (protected + role-based routes)
- axios with an automatic JWT-refresh interceptor
- notistack (toast notifications)

---

## Features

### Admin
- Dashboard with live counts (students, courses, enrollments, active courses)
- Full CRUD for students and the course catalog (capacity, credits,
  semester, instructor, department, status)
- Browse enrollment rosters by course; change enrollment status
- Assign/update grades against a specific enrollment
- Manage user accounts: assign roles (ADMIN/STUDENT), activate/deactivate

### Student
- Dashboard with CGPA, total credits, and course counts
- Browse active courses and self-enroll (capacity-checked server-side)
- View and drop their own enrollments
- View their own grades and a GPA-by-semester breakdown
- View and edit their own profile

### Platform
- Role-based access control enforced **at the service layer**, not just
  the UI — a STUDENT token literally cannot fetch another student's data,
  regardless of what the frontend does
- Stateless JWT access tokens (15 min) + persisted, revocable refresh
  tokens (7 days) with rotation on refresh
- A grade can never exist without a real, verified enrollment behind it
  (grade-service checks with enrollment-service before accepting one)
- Consistent JSON error shape across every service
  (`timestamp`, `status`, `error`, `message`, `path`, `validationErrors`)

---

## Project Structure

```
student-management-system/
├── docs/
│   └── architecture-diagram.svg
├── postman/
│   ├── Phase2-Gateway-Flow.postman_collection.json
│   └── Phase3-Courses-Grades.postman_collection.json
├── frontend/
│   └── student-management-ui/    React 19 + Vite + MUI SPA
└── backend/
    ├── common-lib/                Shared DTOs, exceptions, JWT validation (plain Maven jar)
    ├── api-gateway/                Spring Cloud Gateway
    ├── auth-service/
    ├── student-service/
    ├── course-service/
    ├── enrollment-service/
    ├── grade-service/
    └── docker-compose.yml
```

---

## Setup Instructions

### Prerequisites
- Java 17 JDK
- Maven (or an IDE that bundles it, e.g. IntelliJ)
- Node.js 18+ and npm
- MySQL 8 running locally **or** Docker + Docker Compose

### Option A — Docker Compose (backend, all at once)

```bash
cd backend
docker compose up --build
```

This starts 5 MySQL instances, all 6 Spring Boot services, and the
gateway. Then run the frontend separately (Docker Compose doesn't include
it, so you can iterate on it with hot reload):

```bash
cd ../frontend/student-management-ui
npm install
npm run dev
```

### Option B — Run everything manually

`common-lib` must be installed into your local Maven repo first, since
every backend service depends on it:

```bash
cd backend/common-lib
mvn clean install
```

Then, each in its own terminal (all need `DB_PASSWORD` set to your local
MySQL password):

```bash
cd backend/auth-service        && export DB_PASSWORD=... && mvn spring-boot:run   # :8080
cd backend/student-service     && export DB_PASSWORD=... && mvn spring-boot:run   # :8081
cd backend/enrollment-service  && export DB_PASSWORD=... && mvn spring-boot:run   # :8082
cd backend/course-service      && export DB_PASSWORD=... && mvn spring-boot:run   # :8083
cd backend/grade-service       && export DB_PASSWORD=... && mvn spring-boot:run   # :8084
cd backend/api-gateway         && mvn spring-boot:run                              # :9000

cd frontend/student-management-ui && npm install && npm run dev                    # :5173
```

If using IntelliJ: open `common-lib`, run `mvn install` on it via the
Maven tool window first, then open/run the other modules normally with
`DB_PASSWORD` set in each Run Configuration's environment variables.

### First-time setup: creating an admin

Self-registration (`POST /api/v1/auth/register`) always creates a
**STUDENT** account by design — there's no self-service admin creation.
To get your first admin:
1. Register normally as a student.
2. Connect to `auth_db` and update that user's row: `UPDATE users SET role='ADMIN' WHERE email='...';`
3. Log in again to get a fresh token with the ADMIN role.

---

## API Documentation (Swagger)

Each REST-exposing service has its own Swagger UI, reachable **directly**
on its own port (the gateway routes `/api/v1/**` traffic only, not docs
routes):

| Service              | Swagger UI                                      |
|-----------------------|---------------------------------------------------|
| `auth-service`        | http://localhost:8080/swagger-ui.html             |
| `student-service`     | http://localhost:8081/swagger-ui.html             |
| `enrollment-service`  | http://localhost:8082/swagger-ui.html             |
| `course-service`      | http://localhost:8083/swagger-ui.html             |
| `grade-service`       | http://localhost:8084/swagger-ui.html             |

Note: Swagger UI's own "Try it out" won't have a token attached
automatically — grab an access token from `POST /auth/login` first and
paste it into the "Authorize" button (`Bearer <token>`).

---

## Postman

Two collections are included under `postman/`, both routed through the
gateway on port 9000:
- **Phase2-Gateway-Flow** — register/login/refresh, admin promoting a
  user, student vs admin ownership checks on students/courses/enrollments
- **Phase3-Courses-Grades** — richer course fields, grade assignment by
  enrollment id, GPA/CGPA retrieval, and negative tests (student blocked
  from creating a course, admin blocked from grading a fake enrollment)

---

## Screenshots

Not included in this repository — add your own once you have it running
locally! Good ones to capture: the login screen, the admin dashboard, the
student dashboard, and the course-enrollment flow. Drop them in a
`docs/screenshots/` folder and reference them here.

---

## Docker Commands Cheat Sheet

```bash
# Start everything (backend) in the foreground, rebuilding images
docker compose up --build

# Start in the background
docker compose up -d --build

# View logs for one service
docker compose logs -f enrollment-service

# Stop everything
docker compose down

# Stop everything AND delete the MySQL volumes (full reset)
docker compose down -v

# Rebuild a single service after a code change
docker compose up --build enrollment-service
```

---

## Notes on Production-Readiness

- Constructor injection only (`@RequiredArgsConstructor`), no field injection.
- Refresh tokens are persisted and individually revocable — a stateless
  JWT alone can't support real logout, so the refresh token is opaque and
  stored, with rotation on every refresh.
- Passwords are BCrypt-hashed; plaintext is never stored or logged.
- The JWT secret is a **local-dev placeholder**, identical across all
  services by necessity (they must agree on it to validate each other's
  tokens). Override `JWT_SECRET` with one strong, randomly generated
  value — the same value everywhere — before deploying anywhere real.
- CORS is currently permissive (`allowedOriginPatterns: "*"`) for local
  dev; tighten this before any real deployment.
- Ownership checks live at the **service layer**, not just the
  controller — e.g. `EnrollmentService` re-checks ownership even though
  the controller's `@PreAuthorize` already gates admin-only actions, so a
  future new endpoint can't accidentally skip the check.
- Grades are snapshotted (`credits`, `semester`) at assignment time
  rather than joined live, so GPA calculations stay correct even if a
  course's credit value changes later.

## Known Limitations & Future Enhancements

- **No "list all enrollments" or "list all grades" endpoint** — by
  design, these are scoped by student or by course to avoid an
  unbounded admin query; a paginated aggregate endpoint would be a
  natural next step at real scale.
- **No dedicated admin bootstrap endpoint** — the first admin is created
  by hand-editing the database. A one-time-use bootstrap token or a CLI
  seeding script would be a nice addition.
- **Single shared JWT secret** across services — a real deployment would
  likely move to asymmetric signing (RS256) so only auth-service holds
  the private key and other services only need the public key to verify.
- **No distributed tracing** — adding Spring Cloud Sleuth / Micrometer
  Tracing + Zipkin would make cross-service debugging much easier as the
  system grows.
- **No rate limiting** at the gateway — worth adding for a public deployment.
- **No automated test suite** — the project was validated by running each
  service and exercising it via Postman/curl; unit and integration tests
  (JUnit + Testcontainers for the DB-backed services, React Testing
  Library for the frontend) would be the next investment for a real
  production codebase.
