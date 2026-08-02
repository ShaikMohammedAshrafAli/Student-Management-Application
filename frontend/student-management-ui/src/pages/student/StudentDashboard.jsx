import { useEffect, useState } from 'react';
import { Box, Card, CardContent, Grid, Stack, Typography, Alert } from '@mui/material';
import MenuBookIcon from '@mui/icons-material/MenuBookOutlined';
import CheckCircleIcon from '@mui/icons-material/CheckCircleOutlined';
import GradeIcon from '@mui/icons-material/GradeOutlined';
import EmojiEventsIcon from '@mui/icons-material/EmojiEventsOutlined';
import { useAuth } from '../../context/AuthContext';
import { enrollmentApi } from '../../api/enrollmentApi';
import { gradeApi } from '../../api/gradeApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { extractErrorMessage } from '../../api/axiosClient';

export default function StudentDashboard() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [enrollments, setEnrollments] = useState([]);
  const [gpa, setGpa] = useState(null);

  useEffect(() => {
    if (!user?.studentId) {
      setLoading(false);
      return;
    }
    let cancelled = false;

    (async () => {
      setLoading(true);
      try {
        const [enrollmentData, gpaData] = await Promise.all([
          enrollmentApi.getByStudent(user.studentId),
          gradeApi.getGpa(user.studentId),
        ]);
        if (!cancelled) {
          setEnrollments(enrollmentData);
          setGpa(gpaData);
        }
      } catch (err) {
        if (!cancelled) setError(extractErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [user?.studentId]);

  if (!user?.studentId) {
    return (
      <Alert severity="info" sx={{ maxWidth: 560 }}>
        Your account isn't linked to a student profile yet. Ask an
        administrator to link your account, then check back here.
      </Alert>
    );
  }

  if (loading) return <LoadingSpinner label="Loading your dashboard..." />;
  if (error) return <Alert severity="error">{error}</Alert>;

  const activeCourses = enrollments.filter((e) => e.status === 'CONFIRMED').length;
  const completedCourses = enrollments.filter((e) => e.status === 'COMPLETED').length;

  const cards = [
    { label: 'My Courses', value: activeCourses, icon: <MenuBookIcon />, color: '#3454D1' },
    { label: 'Completed Courses', value: completedCourses, icon: <CheckCircleIcon />, color: '#0EA5A4' },
    { label: 'Total Credits', value: gpa?.totalCredits ?? 0, icon: <GradeIcon />, color: '#7C3AED' },
    { label: 'CGPA', value: gpa?.cgpa?.toFixed(2) ?? '—', icon: <EmojiEventsIcon />, color: '#F59E0B' },
  ];

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Welcome back, {user.fullName?.split(' ')[0]}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Here's where things stand.
      </Typography>

      <Grid container spacing={2.5}>
        {cards.map((c) => (
          <Grid key={c.label} size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Stack direction="row" alignItems="center" spacing={1.5}>
                  <Box
                    sx={{
                      width: 44,
                      height: 44,
                      borderRadius: 2,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      bgcolor: `${c.color}1A`,
                      color: c.color,
                    }}
                  >
                    {c.icon}
                  </Box>
                  <Box>
                    <Typography variant="h5">{c.value}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {c.label}
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {gpa && Object.keys(gpa.gpaBySemester || {}).length > 0 && (
        <Card sx={{ mt: 3 }}>
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
    </Box>
  );
}
