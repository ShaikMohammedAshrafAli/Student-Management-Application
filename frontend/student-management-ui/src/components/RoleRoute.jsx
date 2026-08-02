import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Requires the user to hold a specific role; otherwise redirects to their own home. */
export default function RoleRoute({ role }) {
  const { user } = useAuth();

  if (user?.role !== role) {
    return <Navigate to={user?.role === 'ADMIN' ? '/admin' : '/student'} replace />;
  }
  return <Outlet />;
}
