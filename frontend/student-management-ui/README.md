# StudentHub Frontend

A React 19 + Vite + Material UI single-page app for the Student Management
System, talking to the backend exclusively through `api-gateway` (port 9000).

## Tech Stack

| Concern            | Choice                              |
|---------------------|---------------------------------------|
| Framework           | React 19                              |
| Build tool          | Vite 8                                |
| UI library          | Material UI (MUI) v9                  |
| Routing             | react-router-dom v7                   |
| HTTP client         | axios, with JWT auto-refresh interceptor |
| Notifications       | notistack (toast snackbars)           |

## Project Layout

```
src/
├── api/                 API modules, one per backend service
│   ├── axiosClient.js    axios instance + JWT attach/refresh interceptors
│   ├── authApi.js        auth-service (register/login/refresh/logout) + admin user management
│   ├── studentApi.js     student-service
│   ├── courseApi.js      course-service
│   ├── enrollmentApi.js  enrollment-service
│   └── gradeApi.js       grade-service
├── context/
│   └── AuthContext.jsx   current user, login/register/logout, session persistence
├── components/
│   ├── ProtectedRoute.jsx   redirects to /login if not authenticated
│   ├── RoleRoute.jsx        redirects to the user's own home if role doesn't match
│   ├── layout/AppLayout.jsx  sidebar + top bar, role-based nav
│   └── common/                LoadingSpinner, ConfirmDialog
├── pages/
│   ├── Login.jsx, Register.jsx, NotFound.jsx
│   ├── admin/     AdminDashboard, ManageStudents, ManageCourses,
│   │              ManageEnrollments, ManageGrades, ManageUsers
│   └── student/   StudentDashboard, AvailableCourses, MyCourses,
│                  MyGrades, Profile
├── theme/theme.js        MUI theme customization
└── utils/                constants + localStorage helpers
```

## Running Locally

1. Make sure the backend is running (`api-gateway` on port 9000 at minimum
   - see `../../backend/README.md` and its Docker Compose setup).
2. Install dependencies:
   ```bash
   npm install
   ```
3. Copy the environment template if you haven't already (already done for
   you as `.env` in this delivered copy, pointing at the default gateway URL):
   ```bash
   cp .env.example .env
   ```
4. Start the dev server:
   ```bash
   npm run dev
   ```
5. Open the printed local URL (typically `http://localhost:5173`).

## Key Design Decisions

### JWT auto-refresh (`api/axiosClient.js`)
A response interceptor watches for `401`s. On the first one, it calls
`/auth/refresh` with the stored refresh token, updates both tokens, and
retries the original request. If multiple requests 401 at the same time
(e.g. a dashboard firing several calls at once), they all queue behind a
**single** in-flight refresh call rather than each independently hitting
`/auth/refresh`. If the refresh itself fails, the session is cleared and
the user is redirected to `/login`.

### Response shape awareness
Not every backend service wraps its responses the same way:
- `auth-service` (including `/admin/users/**`) wraps everything in the
  shared `ApiResponse<T>` envelope from `common-lib` -> the API modules
  unwrap `response.data.data`.
- `student-service`, `course-service`, `enrollment-service`, and
  `grade-service` all return raw DTOs directly -> the API modules use
  `response.data`.

This is called out explicitly in `api/gradeApi.js` since it's the easiest
place to get this backwards.

### Route protection
`ProtectedRoute` guards everything behind a login check; `RoleRoute` then
splits `/admin/*` from `/student/*` so a STUDENT account can never even
render an admin page component (on top of the backend's own ownership
checks - this is a UX nicety, not the security boundary; the backend
remains the actual enforcement point).

### Grades workflow
Since grade-service requires an `enrollmentId` (not a raw student/course
pair) to assign a grade, `ManageGrades` lets an admin pick a course, see
its roster (via `enrollmentApi.getByCourse`), and assign/update a grade
per enrollment - mirroring exactly how the backend expects grades to be
created.

### No "list all enrollments" endpoint
The backend intentionally has no global "all enrollments" endpoint (only
by-student and by-course). `AdminDashboard`'s "Total Enrollments" card and
`ManageEnrollments`/`ManageGrades` therefore work by course selection
rather than a flat global list - this matches the real API surface rather
than assuming an endpoint that doesn't exist.

## Build

```bash
npm run build   # outputs to dist/
npm run preview # serve the production build locally
```

This has been verified to build cleanly (`npm run build`) and lint cleanly
(`npm run lint`, 0 errors) in this environment.
