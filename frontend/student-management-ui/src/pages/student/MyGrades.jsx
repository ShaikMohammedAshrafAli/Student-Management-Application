import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  Grid,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useAuth } from '../../context/AuthContext';
import { gradeApi } from '../../api/gradeApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';

export default function MyGrades() {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [grades, setGrades] = useState([]);
  const [gpa, setGpa] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.studentId) {
      setLoading(false);
      return;
    }
    (async () => {
      setLoading(true);
      try {
        const [gradeData, gpaData] = await Promise.all([
          gradeApi.getByStudent(user.studentId),
          gradeApi.getGpa(user.studentId),
        ]);
        setGrades(gradeData);
        setGpa(gpaData);
      } catch (err) {
        enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.studentId]);

  if (!user?.studentId) {
    return (
      <Alert severity="info" sx={{ maxWidth: 560 }}>
        Your account isn't linked to a student profile yet. Ask an
        administrator to link your account.
      </Alert>
    );
  }

  if (loading) return <LoadingSpinner label="Loading your grades..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        My Grades
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Your finalized grades and GPA.
      </Typography>

      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                CGPA
              </Typography>
              <Typography variant="h4">{gpa?.cgpa?.toFixed(2) ?? '—'}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                Total Credits
              </Typography>
              <Typography variant="h4">{gpa?.totalCredits ?? 0}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                Graded Courses
              </Typography>
              <Typography variant="h4">{grades.length}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {gpa && Object.keys(gpa.gpaBySemester || {}).length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" sx={{ mb: 2 }}>
              GPA by Semester
            </Typography>
            <Stack spacing={1}>
              {Object.entries(gpa.gpaBySemester).map(([semester, semesterGpa]) => (
                <Stack key={semester} direction="row" justifyContent="space-between">
                  <Typography variant="body2">{semester}</Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {semesterGpa.toFixed(2)}
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}

      <Card component={Paper}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Course</TableCell>
                <TableCell>Semester</TableCell>
                <TableCell>Credits</TableCell>
                <TableCell align="right">Grade</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {grades.map((g) => (
                <TableRow key={g.id} hover>
                  <TableCell>{g.courseCode} — {g.courseTitle}</TableCell>
                  <TableCell>{g.semester || '—'}</TableCell>
                  <TableCell>{g.credits}</TableCell>
                  <TableCell align="right">
                    <Chip size="small" color="success" label={g.gradePoints.toFixed(1)} />
                  </TableCell>
                </TableRow>
              ))}
              {grades.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No grades recorded yet.
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
