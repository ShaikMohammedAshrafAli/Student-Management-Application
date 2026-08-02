import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Stack,
  Typography,
  Chip,
  List,
  ListItem,
  ListItemText,
  Divider,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/PeopleOutlineOutlined';
import MenuBookIcon from '@mui/icons-material/MenuBookOutlined';
import AssignmentIcon from '@mui/icons-material/AssignmentOutlined';
import GradeIcon from '@mui/icons-material/GradeOutlined';
import { studentApi } from '../../api/studentApi';
import { courseApi } from '../../api/courseApi';
import { enrollmentApi } from '../../api/enrollmentApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';

const CARD_META = [
  { key: 'students', label: 'Total Students', icon: <PeopleIcon />, color: '#3454D1' },
  { key: 'courses', label: 'Total Courses', icon: <MenuBookIcon />, color: '#7C3AED' },
  { key: 'enrollments', label: 'Total Enrollments', icon: <AssignmentIcon />, color: '#0EA5A4' },
  { key: 'activeCourses', label: 'Active Courses', icon: <GradeIcon />, color: '#F59E0B' },
];

export default function AdminDashboard() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ students: 0, courses: 0, enrollments: 0, activeCourses: 0 });
  const [recentCourses, setRecentCourses] = useState([]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      try {
        const [students, courses] = await Promise.all([studentApi.list(), courseApi.list()]);

        // No dedicated "all enrollments" admin endpoint exists yet, so the
        // total is aggregated client-side across each course. Fine for a
        // demo-scale dataset; a dedicated aggregate endpoint would be the
        // next step for a larger deployment.
        const enrollmentCounts = await Promise.allSettled(
          courses.map((c) => enrollmentApi.getByCourse(c.id))
        );
        const totalEnrollments = enrollmentCounts.reduce(
          (sum, result) => sum + (result.status === 'fulfilled' ? result.value.length : 0),
          0
        );

        if (!cancelled) {
          setStats({
            students: students.length,
            courses: courses.length,
            enrollments: totalEnrollments,
            activeCourses: courses.filter((c) => c.status === 'ACTIVE').length,
          });
          setRecentCourses(courses.slice(-5).reverse());
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <LoadingSpinner label="Loading dashboard..." />;

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 0.5 }}>
        Admin Dashboard
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        An overview of students, courses, and enrollment activity.
      </Typography>

      <Grid container spacing={2.5}>
        {CARD_META.map((meta) => (
          <Grid key={meta.key} size={{ xs: 12, sm: 6, md: 3 }}>
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
                      bgcolor: `${meta.color}1A`,
                      color: meta.color,
                    }}
                  >
                    {meta.icon}
                  </Box>
                  <Box>
                    <Typography variant="h5">{stats[meta.key]}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {meta.label}
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Recently Added Courses
          </Typography>
          {recentCourses.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No courses yet.{' '}
              <RouterLink to="/admin/courses">Create one</RouterLink>
            </Typography>
          ) : (
            <List disablePadding>
              {recentCourses.map((course, idx) => (
                <Box key={course.id}>
                  <ListItem
                    disableGutters
                    secondaryAction={
                      <Chip
                        size="small"
                        label={course.status}
                        color={course.status === 'ACTIVE' ? 'success' : 'default'}
                        variant="outlined"
                      />
                    }
                  >
                    <ListItemText
                      primary={`${course.courseCode} — ${course.title}`}
                      secondary={`${course.credits} credits · Capacity ${course.capacity}${
                        course.semester ? ` · ${course.semester}` : ''
                      }`}
                    />
                  </ListItem>
                  {idx < recentCourses.length - 1 && <Divider component="li" />}
                </Box>
              ))}
            </List>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
