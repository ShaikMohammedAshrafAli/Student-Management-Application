import { useEffect, useState } from 'react';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useAuth } from '../../context/AuthContext';
import { studentApi } from '../../api/studentApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';

export default function Profile() {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', phoneNumber: '', dateOfBirth: '' });

  const load = async () => {
    setLoading(true);
    try {
      const data = await studentApi.getById(user.studentId);
      setProfile(data);
      setForm({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        phoneNumber: data.phoneNumber || '',
        dateOfBirth: data.dateOfBirth || '',
      });
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.studentId) load();
    else setLoading(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.studentId]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await studentApi.update(user.studentId, form);
      enqueueSnackbar('Profile updated', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  if (!user?.studentId) {
    return (
      <Alert severity="info" sx={{ maxWidth: 560 }}>
        Your account isn't linked to a student profile yet. Ask an
        administrator to link your account.
      </Alert>
    );
  }

  if (loading) return <LoadingSpinner label="Loading your profile..." />;

  return (
    <Box sx={{ maxWidth: 560 }}>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Profile
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        View and update your personal information.
      </Typography>

      <Card>
        <CardContent sx={{ p: 3 }}>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
            <Avatar sx={{ width: 56, height: 56, bgcolor: 'secondary.main', fontSize: 20 }}>
              {(profile?.firstName || '?').charAt(0).toUpperCase()}
            </Avatar>
            <Box>
              <Typography variant="h6">
                {profile?.firstName} {profile?.lastName}
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="body2" color="text.secondary">
                  {profile?.email}
                </Typography>
                <Chip size="small" label={profile?.status} variant="outlined" />
              </Stack>
            </Box>
          </Stack>

          <Stack spacing={2}>
            <Stack direction="row" spacing={2}>
              <TextField
                label="First Name"
                fullWidth
                value={form.firstName}
                onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
              />
              <TextField
                label="Last Name"
                fullWidth
                value={form.lastName}
                onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
              />
            </Stack>
            <TextField label="Email" fullWidth value={profile?.email || ''} disabled helperText="Contact an admin to change your email" />
            <TextField
              label="Phone Number"
              fullWidth
              value={form.phoneNumber}
              onChange={(e) => setForm((f) => ({ ...f, phoneNumber: e.target.value }))}
            />
            <TextField
              label="Date of Birth"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={form.dateOfBirth}
              onChange={(e) => setForm((f) => ({ ...f, dateOfBirth: e.target.value }))}
            />
            <Box>
              <Button variant="contained" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </Button>
            </Box>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
