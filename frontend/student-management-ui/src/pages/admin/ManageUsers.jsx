import { useEffect, useState } from 'react';
import {
  Box,
  Card,
  Chip,
  MenuItem,
  Paper,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { adminApi } from '../../api/authApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';
import { ROLES } from '../../utils/constants';
import { useAuth } from '../../context/AuthContext';

export default function ManageUsers() {
  const { enqueueSnackbar } = useSnackbar();
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      setUsers(await adminApi.listUsers());
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleRoleChange = async (userId, role) => {
    setBusyId(userId);
    try {
      await adminApi.assignRole(userId, role);
      enqueueSnackbar('Role updated', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setBusyId(null);
    }
  };

  const handleToggleEnabled = async (userId, enabled) => {
    setBusyId(userId);
    try {
      await adminApi.setEnabled(userId, enabled);
      enqueueSnackbar(enabled ? 'User activated' : 'User deactivated', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <LoadingSpinner label="Loading users..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Users
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Assign roles and activate/deactivate accounts.
      </Typography>

      <Card component={Paper}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Student ID</TableCell>
                <TableCell align="center">Active</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((u) => (
                <TableRow key={u.id} hover>
                  <TableCell>{u.fullName}</TableCell>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>
                    <TextField
                      select
                      size="small"
                      value={u.role}
                      disabled={busyId === u.id || u.id === currentUser?.userId}
                      onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      sx={{ minWidth: 130 }}
                    >
                      {Object.values(ROLES).map((r) => (
                        <MenuItem key={r} value={r}>
                          {r}
                        </MenuItem>
                      ))}
                    </TextField>
                  </TableCell>
                  <TableCell>
                    {u.studentId ? <Chip size="small" label={`#${u.studentId}`} variant="outlined" /> : '—'}
                  </TableCell>
                  <TableCell align="center">
                    <Switch
                      checked={u.enabled}
                      disabled={busyId === u.id || u.id === currentUser?.userId}
                      onChange={(e) => handleToggleEnabled(u.id, e.target.checked)}
                    />
                  </TableCell>
                </TableRow>
              ))}
              {users.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No users found.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>
    </Box>
  );
}
