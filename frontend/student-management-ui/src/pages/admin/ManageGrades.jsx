import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
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
import { gradeApi } from '../../api/gradeApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';

export default function ManageGrades() {
  const { enqueueSnackbar } = useSnackbar();
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [roster, setRoster] = useState([]); // [{ enrollment, grade }]
  const [loadingCourses, setLoadingCourses] = useState(true);
  const [loadingRoster, setLoadingRoster] = useState(false);

  const [dialogEnrollment, setDialogEnrollment] = useState(null);
  const [gradePoints, setGradePoints] = useState('');
  const [saving, setSaving] = useState(false);

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

  const loadRoster = async (courseId) => {
    setLoadingRoster(true);
    try {
      const enrollments = await enrollmentApi.getByCourse(courseId);
      // Grade-service has no "grades by enrollment" endpoint, so we fetch
      // each student's full grade list and match this course's entry.
      const withGrades = await Promise.all(
        enrollments.map(async (enrollment) => {
          try {
            const grades = await gradeApi.getByStudent(enrollment.studentId);
            const grade = grades.find((g) => g.courseId === Number(courseId)) || null;
            return { enrollment, grade };
          } catch {
            return { enrollment, grade: null };
          }
        })
      );
      setRoster(withGrades);
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setLoadingRoster(false);
    }
  };

  useEffect(() => {
    if (selectedCourseId) loadRoster(selectedCourseId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCourseId]);

  const openGradeDialog = (enrollment, existingGrade) => {
    setDialogEnrollment(enrollment);
    setGradePoints(existingGrade ? String(existingGrade.gradePoints) : '');
  };

  const handleAssign = async () => {
    setSaving(true);
    try {
      await gradeApi.assign(dialogEnrollment.id, Number(gradePoints));
      enqueueSnackbar('Grade saved', { variant: 'success' });
      setDialogEnrollment(null);
      loadRoster(selectedCourseId);
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  if (loadingCourses) return <LoadingSpinner label="Loading courses..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Grades
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Assign or update grades for a course's confirmed/completed enrollments.
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
            {c.courseCode} — {c.title} ({c.credits} credits)
          </MenuItem>
        ))}
      </TextField>

      {loadingRoster ? (
        <LoadingSpinner label="Loading roster..." />
      ) : (
        <Card component={Paper}>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Student</TableCell>
                  <TableCell>Enrollment Status</TableCell>
                  <TableCell>Current Grade</TableCell>
                  <TableCell align="right">Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {roster.map(({ enrollment, grade }) => {
                  const gradable = ['CONFIRMED', 'COMPLETED'].includes(enrollment.status);
                  return (
                    <TableRow key={enrollment.id} hover>
                      <TableCell>
                        {enrollment.student
                          ? `${enrollment.student.firstName} ${enrollment.student.lastName}`
                          : `Student #${enrollment.studentId}`}
                      </TableCell>
                      <TableCell>
                        <Chip size="small" label={enrollment.status} variant="outlined" />
                      </TableCell>
                      <TableCell>
                        {grade ? (
                          <Chip size="small" color="success" label={grade.gradePoints.toFixed(1)} />
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            Not graded
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={!gradable}
                          onClick={() => openGradeDialog(enrollment, grade)}
                        >
                          {grade ? 'Update' : 'Assign'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
                {roster.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                      No enrollments for this course yet.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      )}

      <Dialog open={!!dialogEnrollment} onClose={() => setDialogEnrollment(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Assign Grade</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {dialogEnrollment?.student
                ? `${dialogEnrollment.student.firstName} ${dialogEnrollment.student.lastName}`
                : `Student #${dialogEnrollment?.studentId}`}
            </Typography>
            <TextField
              label="Grade Points (0-10)"
              type="number"
              inputProps={{ min: 0, max: 10, step: 0.1 }}
              value={gradePoints}
              onChange={(e) => setGradePoints(e.target.value)}
              autoFocus
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogEnrollment(null)} color="inherit">
            Cancel
          </Button>
          <Button onClick={handleAssign} variant="contained" disabled={saving || gradePoints === ''}>
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
