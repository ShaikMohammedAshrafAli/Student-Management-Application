import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Stack,
  Typography,
  Alert,
} from '@mui/material';
import { useSnackbar } from 'notistack';
import { useAuth } from '../../context/AuthContext';
import { courseApi } from '../../api/courseApi';
import { enrollmentApi } from '../../api/enrollmentApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';

export default function AvailableCourses() {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const [courses, setCourses] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [enrollingId, setEnrollingId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const [courseData, enrollmentData] = await Promise.all([
        courseApi.list(),
        user?.studentId ? enrollmentApi.getByStudent(user.studentId) : Promise.resolve([]),
      ]);
      setCourses(courseData.filter((c) => c.status === 'ACTIVE'));
      setMyEnrollments(enrollmentData);
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.studentId]);

  const handleEnroll = async (courseId) => {
    setEnrollingId(courseId);
    try {
      await enrollmentApi.enroll(user.studentId, courseId);
      enqueueSnackbar('Enrolled successfully', { variant: 'success' });
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setEnrollingId(null);
    }
  };

  if (!user?.studentId) {
    return (
      <Alert severity="info" sx={{ maxWidth: 560 }}>
        Your account isn't linked to a student profile yet. Ask an
        administrator to link your account before enrolling in courses.
      </Alert>
    );
  }

  if (loading) return <LoadingSpinner label="Loading courses..." />;

  const enrolledCourseIds = new Set(
    myEnrollments.filter((e) => e.status !== 'DROPPED' && e.status !== 'REJECTED').map((e) => e.courseId)
  );

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Available Courses
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Browse active courses and enroll.
      </Typography>

      <Grid container spacing={2.5}>
        {courses.map((course) => {
          const alreadyEnrolled = enrolledCourseIds.has(course.id);
          return (
            <Grid key={course.id} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ mb: 1 }}>
                    <Chip size="small" label={course.courseCode} color="primary" variant="outlined" />
                    <Typography variant="caption" color="text.secondary">
                      {course.credits} credits
                    </Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ mb: 0.5 }}>
                    {course.title}
                  </Typography>
                  {course.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                      {course.description}
                    </Typography>
                  )}
                  <Stack spacing={0.5}>
                    {course.instructor && (
                      <Typography variant="caption" color="text.secondary">
                        Instructor: {course.instructor}
                      </Typography>
                    )}
                    {course.department && (
                      <Typography variant="caption" color="text.secondary">
                        Department: {course.department}
                      </Typography>
                    )}
                    {course.semester && (
                      <Typography variant="caption" color="text.secondary">
                        Semester: {course.semester}
                      </Typography>
                    )}
                  </Stack>
                </CardContent>
                <Box sx={{ p: 2, pt: 0 }}>
                  <Button
                    fullWidth
                    variant={alreadyEnrolled ? 'outlined' : 'contained'}
                    disabled={alreadyEnrolled || enrollingId === course.id}
                    onClick={() => handleEnroll(course.id)}
                  >
                    {alreadyEnrolled ? 'Already Enrolled' : enrollingId === course.id ? 'Enrolling...' : 'Enroll'}
                  </Button>
                </Box>
              </Card>
            </Grid>
          );
        })}
        {courses.length === 0 && (
          <Grid size={12}>
            <Typography variant="body2" color="text.secondary">
              No active courses available right now.
            </Typography>
          </Grid>
        )}
      </Grid>
    </Box>
  );
}
