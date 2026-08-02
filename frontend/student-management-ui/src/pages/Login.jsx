import { useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Link,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import { useAuth } from '../context/AuthContext';
import { extractErrorMessage } from '../api/axiosClient';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const user = await login(form.email, form.password);
      const redirectTo = location.state?.from?.pathname || (user.role === 'ADMIN' ? '/admin' : '/student');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        px: 2,
      }}
    >
      <Card sx={{ maxWidth: 420, width: '100%' }}>
        <CardContent sx={{ p: 4 }}>
          <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
            <SchoolIcon color="primary" sx={{ fontSize: 40 }} />
            <Typography variant="h5">Welcome back</Typography>
            <Typography variant="body2" color="text.secondary">
              Sign in to StudentHub
            </Typography>
          </Stack>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Email"
                type="email"
                value={form.email}
                onChange={handleChange('email')}
                required
                fullWidth
                autoFocus
              />
              <TextField
                label="Password"
                type="password"
                value={form.password}
                onChange={handleChange('password')}
                required
                fullWidth
              />
              <Button type="submit" variant="contained" size="large" disabled={submitting} fullWidth>
                {submitting ? 'Signing in...' : 'Sign In'}
              </Button>
            </Stack>
          </Box>

          <Typography variant="body2" align="center" sx={{ mt: 3 }}>
            Don't have an account?{' '}
            <Link component={RouterLink} to="/register">
              Register
            </Link>
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}
