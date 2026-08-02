# Student Management System — Enterprise Edition (Phases 1–4)

This is the enterprise upgrade of the original `student-management-microservices`
project. It's being built in phases so each part is fully working before the
next is added — see the roadmap below.

## What's in Phase 1 + 2 + 3 + 4

```
student-management-system/
├── postman/
│   ├── Phase2-Gateway-Flow.postman_collection.json
│   └── Phase3-Courses-Grades.postman_collection.json
├── frontend/
│   └── student-management-ui/  <- NEW (Phase 4): React 19 + Vite + MUI SPA
└── backend/
    ├── common-lib/         <- shared DTOs, exceptions, constants, JWT validation
    ├── auth-service/        <- registration, login, JWT, roles, user management
    ├── api-gateway/         <- single entry point, routing, JWT check
    ├── student-service/     <- JWT-secured, ownership enforced
    ├── enrollment-service/  <- JWT-secured, calls course-service + student-service
    ├── course-service/      <- course catalog (own DB)
    ├── grade-service/       <- grades + GPA/CGPA (own DB)
    └── docker-compose.yml   <- runs everything above in one command
```

### api-gateway (port 9000)
Built with Spring Cloud Gateway (reactive). This is the single entry point
every client (Postman, the future React frontend) should call instead of
hitting services directly:

| Route prefix                          | Forwards to        |
|-----------------------------------------|----------------------|
| `/api/v1/auth/**`, `/api/v1/admin/users/**` | auth-service (8080) |
| `/api/v1/students/**`                  | student-service (8081) |
| `/api/v1/enrollments/**`               | enrollment-service (8082) |
| `/api/v1/courses/**`                   | course-service (8083) |
| `/api/v1/grades/**`                    | grade-service (8084) |

A `JwtValidationGlobalFilter` runs before routing: public paths
(`register`, `login`, `refresh`, `actuator/health`) pass straight through;
everything else must carry a syntactically valid, unexpired Bearer token
or the gateway rejects it with `401` before it ever reaches a backend
service. This is a coarse check only (signature + expiry) - fine-grained
authorization (roles, ownership) still happens in the owning service.

### student-service & enrollment-service — JWT-secured, ownership enforced
Both services gained:
- `spring-boot-starter-security` + a `JwtAuthenticationFilter` that
  validates the same JWT auth-service issues (shared `jwt.secret`) and
  populates `SecurityContextHolder` with a `JwtPrincipal` (userId, email,
  role, studentId).
- **Ownership enforcement:**
  - A **STUDENT** can only view/update **their own** student profile
    (`student-service`) and can only view/drop **their own** enrollments,
    and can only ever enroll **themselves** - the JWT's `studentId` claim
    is authoritative even if a different id is sent in the request body
    (`enrollment-service`).
  - An **ADMIN** can do everything: manage all students, courses, and
    enrollments, and assign grades.
- **Pass-through auth:** when enrollment-service needs to verify a student
  exists (via `StudentClient`), it forwards the *original caller's* bearer
  token to student-service rather than using a service-account credential.
  This means student-service applies the exact same ownership rule
  regardless of whether the call came directly or via enrollment-service.

## Building locally (no Docker required)

`common-lib` must be installed first - every other module depends on it:

```bash
cd backend/common-lib
mvn clean install
```

Then run each service in its own terminal (all need `DB_PASSWORD` set;
`JWT_SECRET` is optional locally since all services share the same
built-in default):

```bash
# Terminal 1
cd backend/auth-service
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run          # port 8080

# Terminal 2
cd backend/student-service
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run          # port 8081

# Terminal 3
cd backend/enrollment-service
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run          # port 8082

# Terminal 4
cd backend/course-service
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run          # port 8083

# Terminal 5
cd backend/grade-service
export DB_PASSWORD=your_mysql_password
mvn spring-boot:run          # port 8084

# Terminal 6
cd backend/api-gateway
mvn spring-boot:run          # port 9000
```

If you're using IntelliJ: open `common-lib` and run `mvn install` on it
first (Maven tool window), then open/run the other six modules normally,
each with `DB_PASSWORD` (and `allowPublicKeyRetrieval=true`, already in
the JDBC URL) set per the earlier setup notes.

## Testing the full Phase 2 flow

Import `postman/Phase2-Gateway-Flow.postman_collection.json` - everything
routes through the gateway on port 9000. Suggested order:

1. **Register** -> creates a STUDENT account, returns access + refresh tokens
2. Since self-registration only creates STUDENT accounts, **promote your
   first admin manually**: insert a row directly into `auth_db.users` with
   `role='ADMIN'` (or register a second account, then use MySQL to flip its
   `role` column to `ADMIN` - there's no bootstrapping endpoint by design,
   since admin creation shouldn't be self-service)
3. **Login as that admin** -> save the `accessToken` as `adminAccessToken`
4. **Admin creates a student profile**, a **course**
5. **Login as the student** -> save the `accessToken` as `studentAccessToken`
6. **Student enrolls themselves**, views their own profile/enrollments
7. Try having the student request **someone else's** student id or
   enrollment - confirm you get a clean `403 Forbidden`, not a 500 or a
   silent data leak

```bash
# Quick curl smoke test (through the gateway)
curl -X POST http://localhost:9000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Asha Rao","email":"asha.rao@example.com","password":"SecurePass123"}'

curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"asha.rao@example.com","password":"SecurePass123"}'
```

## What's new in Phase 3

```
backend/
├── course-service/   ← NEW: course catalog, split out of enrollment-service
└── grade-service/    ← NEW: grade assignment + GPA/CGPA calculation
```

### course-service (port 8083)
Course ownership moved out of enrollment-service into its own bounded
context with its own database (`course_db`). Beyond the original fields
(code, title, credits, capacity), courses now also track **semester**,
**instructor**, **department**, and a **status** (`ACTIVE` / `INACTIVE` /
`COMPLETED` / `CANCELLED`). Any authenticated user can browse courses;
only ADMIN can create/update/delete one.

### enrollment-service, refactored
`enrollment-service` no longer owns a `Course` entity or table — it holds
only `studentId` + `courseId` references and calls `course-service` (via
a new `CourseClient`, forwarding the caller's own bearer token, same
pattern as `StudentClient`) whenever it needs course details or a
capacity check. The legacy `PATCH /enrollments/{id}/grade` endpoint and
the `grade` column still work for backward compatibility, but they're
now marked `@Deprecated` — **grade-service is the source of truth for
grades going forward.**

### grade-service (port 8084)
- `POST /api/v1/grades` (ADMIN only) — assigns a grade **against an
  existing enrollment id**, not a raw `(studentId, courseId)` pair. This
  guarantees a grade can never exist without a real, verified enrollment
  behind it (grade-service calls `enrollment-service` to check the
  enrollment exists and is `CONFIRMED`/`COMPLETED` before accepting a
  grade). `credits` and `semester` are snapshotted from `course-service`
  at assignment time, so GPA math stays correct even if a course's credit
  value changes later.
- `GET /api/v1/grades/student/{studentId}` — all grades for a student
  (ADMIN or the owning student only).
- `GET /api/v1/grades/student/{studentId}/gpa` — **CGPA** (credit-weighted
  average across every grade) plus a **semester-wise GPA breakdown**
  (ADMIN or the owning student only).

GPA/CGPA formula: `sum(gradePoints × credits) / sum(credits)`, computed
per semester and overall, on a 0.0–10.0 grade-point scale.

## Testing the full Phase 3 flow

```bash
# Continuing from the Phase 2 flow (admin + student already logged in)...

# Admin creates a richer course
curl -X POST http://localhost:9000/api/v1/courses \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"courseCode":"CS201","title":"Data Structures","credits":4,"capacity":30,"semester":"FALL2026","instructor":"Dr. Iyer","department":"Computer Science"}'

# Student enrolls (enrollment-service calls course-service internally for capacity)
curl -X POST http://localhost:9000/api/v1/enrollments \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "Content-Type: application/json" \
  -d '{"studentId":1,"courseId":1}'

# Admin assigns a grade against that enrollment (enrollment id, not raw ids)
curl -X POST http://localhost:9000/api/v1/grades \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"enrollmentId":1,"gradePoints":8.7}'

# Student checks their own CGPA
curl http://localhost:9000/api/v1/grades/student/1/gpa \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

## What's new in Phase 4

```
frontend/student-management-ui/   <- React 19 + Vite + MUI single-page app
```

A full frontend against everything built in Phases 1–3, talking only to
`api-gateway` (never to individual services directly). See
`frontend/student-management-ui/README.md` for the detailed breakdown;
the short version:

- **Login / Register**, with JWT access + refresh tokens persisted in
  `localStorage` and an axios interceptor that transparently refreshes
  an expired access token (queuing concurrent requests behind a single
  refresh call) before retrying.
- **Role-based routing:** `/admin/*` and `/student/*` are separate route
  trees; a STUDENT account can't even render an admin page component
  (the backend's ownership checks remain the real security boundary —
  this is purely a UX guard).
- **Admin:** dashboard with live counts, full CRUD for students and
  courses, an enrollment roster browser (by course), a grade-assignment
  screen (by course roster, matching grade-service's enrollment-based
  contract), and user role/status management.
- **Student:** dashboard with CGPA/credits/course counts, browse +
  self-enroll in available courses, view/drop own enrollments, view own
  grades and GPA-by-semester, edit own profile.
- Toast notifications (notistack) on every mutation, confirmation dialogs
  before destructive actions (delete/drop), loading states throughout.

### Running the full stack end-to-end

```bash
# Terminal 1-7: start common-lib (mvn install) + all 6 backend services
# + api-gateway, exactly as described above.

# Terminal 8: the frontend
cd frontend/student-management-ui
npm install
npm run dev
```

Then open the printed local URL, register a student account, promote an
admin the same way as before (flip a row in `auth_db.users`), and log in
as both to see each role's experience.

This has been verified to `npm run build` and `npm run lint` (0 errors)
successfully in the environment used to generate it.

## Roadmap (upcoming phases)

- ~~**Phase 2:** `api-gateway` + wire JWT security into `student-service`
  and `enrollment-service`~~ ✅ Done
- ~~**Phase 3:** `course-service` (separated out of enrollment-service,
  with semester/instructor/department/status) + `grade-service`
  (GPA/CGPA calculation)~~ ✅ Done
- ~~**Phase 4:** React + Vite + Material UI frontend with protected routes,
  admin & student dashboards~~ ✅ Done
- **Phase 5:** Swagger across every service, architecture diagram, final
  polished README

## Notes on Production-Readiness (Phase 1 + 2 + 3)

- Constructor injection only (`@RequiredArgsConstructor`), no field injection.
- Refresh tokens are persisted and individually revocable — a stateless JWT
  alone can't support real logout, so the refresh token is opaque + stored.
- Passwords are BCrypt-hashed; plaintext is never stored or logged.
- CORS is configured centrally in each service's `SecurityConfig` (and
  again at the gateway) — currently permissive for local dev; tighten
  `allowedOriginPatterns` before any real deployment.
- The JWT secret in `application.properties`/`application.yml` is a
  **local-dev placeholder** shared identically across auth-service,
  student-service, enrollment-service, course-service, grade-service, and
  api-gateway (they must all agree on the same secret to validate each
  other's tokens). Override `JWT_SECRET` with one strong, randomly
  generated value — the same value everywhere — in any real environment.
- **Defense in depth:** the gateway checks JWT validity before routing;
  each downstream service independently re-validates the same JWT rather
  than trusting the gateway blindly. A request that reaches student-service
  or enrollment-service directly (bypassing the gateway) is still fully
  protected on its own.
- **Ownership checks live at the service layer, not just the controller:**
  in enrollment-service, `EnrollmentService` re-checks ownership even
  though the controller does too, so a future new endpoint can't
  accidentally skip the check by forgetting a `@PreAuthorize` annotation.
- **Grades can't exist without a real enrollment:** grade-service assigns
  a grade against an `enrollmentId`, not a raw `(studentId, courseId)`
  pair - it calls enrollment-service to confirm that enrollment exists
  and is in a gradable state (`CONFIRMED`/`COMPLETED`) before accepting
  the grade. This closes off a whole class of bad states (grades for
  enrollments that don't exist, or that were dropped).
- **Snapshotting over live joins:** both `CartItem`-style snapshotting
  (from the SmartCart project) and `Grade.credits`/`Grade.semester` here
  follow the same principle - copy the value you depend on for a
  calculation into your own row at the moment it's used, rather than
  re-fetching it live every time. This keeps GPA calculations stable and
  correct even if a course's credit value is edited after the fact.
