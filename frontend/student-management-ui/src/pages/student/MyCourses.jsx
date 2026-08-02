import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  Chip,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutlineOutlined';
import { useSnackbar } from 'notistack';
import { useAuth } from '../../context/AuthContext';
import { enrollmentApi } from '../../api/enrollmentApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { extractErrorMessage } from '../../api/axiosClient';

const statusColor = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'error',
  DROPPED: 'default',
  COMPLETED: 'info',
};

export default function MyCourses() {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [confirmDropId, setConfirmDropId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      setEnrollments(await enrollmentApi.getByStudent(user.studentId));
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

  const handleDrop = async () => {
    try {
      await enrollmentApi.drop(confirmDropId);
      enqueueSnackbar('Course dropped', { variant: 'success' });
      setConfirmDropId(null);
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
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

  if (loading) return <LoadingSpinner label="Loading your courses..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        My Courses
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Everything you're currently enrolled in, plus your enrollment history.
      </Typography>

      <Card component={Paper}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Course</TableCell>
                <TableCell>Instructor</TableCell>
                <TableCell>Credits</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Enrolled</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.map((e) => (
                <TableRow key={e.id} hover>
                  <TableCell>
                    {e.course ? `${e.course.courseCode} — ${e.course.title}` : `Course #${e.courseId}`}
                  </TableCell>
                  <TableCell>{e.course?.instructor || '—'}</TableCell>
                  <TableCell>{e.course?.credits ?? '—'}</TableCell>
                  <TableCell>
                    <Chip size="small" label={e.status} color={statusColor[e.status] || 'default'} variant="outlined" />
                  </TableCell>
                  <TableCell>{e.enrolledAt ? new Date(e.enrolledAt).toLocaleDateString() : '—'}</TableCell>
                  <TableCell align="right">
                    {(e.status === 'CONFIRMED' || e.status === 'PENDING') && (
                      <Tooltip title="Drop course">
                        <IconButton size="small" color="error" onClick={() => setConfirmDropId(e.id)}>
                          <RemoveCircleOutlineIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {enrollments.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    You're not enrolled in any courses yet.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      <ConfirmDialog
        open={!!confirmDropId}
        title="Drop this course?"
        message="You can be re-enrolled later if there's still room, but your current progress in this course will be marked dropped."
        confirmLabel="Drop Course"
        destructive
        onConfirm={handleDrop}
        onClose={() => setConfirmDropId(null)}
      />
    </Box>
  );
}
