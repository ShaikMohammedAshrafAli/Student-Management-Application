import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';
import AppLayout from './components/layout/AppLayout';

import Login from './pages/Login';
import Register from './pages/Register';
import NotFound from './pages/NotFound';

import AdminDashboard from './pages/admin/AdminDashboard';
import ManageStudents from './pages/admin/ManageStudents';
import ManageCourses from './pages/admin/ManageCourses';
import ManageEnrollments from './pages/admin/ManageEnrollments';
import ManageGrades from './pages/admin/ManageGrades';
import ManageUsers from './pages/admin/ManageUsers';

import StudentDashboard from './pages/student/StudentDashboard';
import AvailableCourses from './pages/student/AvailableCourses';
import MyCourses from './pages/student/MyCourses';
import MyGrades from './pages/student/MyGrades';
import Profile from './pages/student/Profile';

function HomeRedirect() {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={isAdmin ? '/admin' : '/student'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<HomeRedirect />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route element={<RoleRoute role="ADMIN" />}>
            <Route path="/admin" element={<AdminDashboard />} />
            <Route path="/admin/students" element={<ManageStudents />} />
            <Route path="/admin/courses" element={<ManageCourses />} />
            <Route path="/admin/enrollments" element={<ManageEnrollments />} />
            <Route path="/admin/grades" element={<ManageGrades />} />
            <Route path="/admin/users" element={<ManageUsers />} />
          </Route>

          <Route element={<RoleRoute role="STUDENT" />}>
            <Route path="/student" element={<StudentDashboard />} />
            <Route path="/student/courses" element={<AvailableCourses />} />
            <Route path="/student/my-courses" element={<MyCourses />} />
            <Route path="/student/grades" element={<MyGrades />} />
            <Route path="/student/profile" element={<Profile />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
