import { useEffect, useState } from 'react';
import {
  Box,
  Card,
  Chip,
  MenuItem,
  Paper,
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
import { courseApi } from '../../api/courseApi';
import { enrollmentApi } from '../../api/enrollmentApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';
import { ENROLLMENT_STATUS } from '../../utils/constants';

const statusColor = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  REJECTED: 'error',
  DROPPED: 'default',
  COMPLETED: 'info',
};

export default function ManageEnrollments() {
  const { enqueueSnackbar } = useSnackbar();
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [enrollments, setEnrollments] = useState([]);
  const [loadingCourses, setLoadingCourses] = useState(true);
  const [loadingEnrollments, setLoadingEnrollments] = useState(false);
  const [updatingId, setUpdatingId] = useState(null);

  useEffect(() => {
    (async () => {
      setLoadingCourses(true);
      try {
        const data = await courseApi.list();
        setCourses(data);
        if (data.length > 0) setSelectedCourseId(data[0].id);
      } catch (err) {
        enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
      } finally {
        setLoadingCourses(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedCourseId) return;
    (async () => {
      setLoadingEnrollments(true);
      try {
        const data = await enrollmentApi.getByCourse(selectedCourseId);
        setEnrollments(data);
      } catch (err) {
        enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
      } finally {
        setLoadingEnrollments(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCourseId]);

  const handleStatusChange = async (enrollmentId, status) => {
    setUpdatingId(enrollmentId);
    try {
      await enrollmentApi.updateStatus(enrollmentId, status);
      enqueueSnackbar('Enrollment status updated', { variant: 'success' });
      const data = await enrollmentApi.getByCourse(selectedCourseId);
      setEnrollments(data);
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setUpdatingId(null);
    }
  };

  if (loadingCourses) return <LoadingSpinner label="Loading courses..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Enrollments
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Browse enrollments by course and manage their status.
      </Typography>

      <TextField
        select
        label="Course"
        value={selectedCourseId}
        onChange={(e) => setSelectedCourseId(e.target.value)}
        sx={{ mb: 3, minWidth: 320 }}
      >
        {courses.map((c) => (
          <MenuItem key={c.id} value={c.id}>
            {c.courseCode} — {c.title}
          </MenuItem>
        ))}
      </TextField>

      {loadingEnrollments ? (
        <LoadingSpinner label="Loading enrollments..." />
      ) : (
        <Card component={Paper}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Student</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Enrolled At</TableCell>
                  <TableCell align="right">Change Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {enrollments.map((e) => (
                  <TableRow key={e.id} hover>
                    <TableCell>
                      {e.student ? `${e.student.firstName} ${e.student.lastName}` : `Student #${e.studentId}`}
                    </TableCell>
                    <TableCell>{e.student?.email || '—'}</TableCell>
                    <TableCell>
                      <Chip size="small" label={e.status} color={statusColor[e.status] || 'default'} variant="outlined" />
                    </TableCell>
                    <TableCell>{e.enrolledAt ? new Date(e.enrolledAt).toLocaleDateString() : '—'}</TableCell>
                    <TableCell align="right">
                      <TextField
                        select
                        size="small"
                        value={e.status}
                        disabled={updatingId === e.id}
                        onChange={(ev) => handleStatusChange(e.id, ev.target.value)}
                        sx={{ minWidth: 150 }}
                      >
                        {Object.values(ENROLLMENT_STATUS).map((s) => (
                          <MenuItem key={s} value={s}>
                            {s}
                          </MenuItem>
                        ))}
                      </TextField>
                    </TableCell>
                  </TableRow>
                ))}
                {enrollments.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      No students enrolled in this course yet.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}
    </Box>
  );
}
