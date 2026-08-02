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
  IconButton,
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
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/EditOutlined';
import DeleteIcon from '@mui/icons-material/DeleteOutlineOutlined';
import { useSnackbar } from 'notistack';
import { courseApi } from '../../api/courseApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { extractErrorMessage } from '../../api/axiosClient';
import { COURSE_STATUS } from '../../utils/constants';

const emptyForm = {
  courseCode: '',
  title: '',
  description: '',
  credits: 3,
  capacity: 30,
  semester: '',
  instructor: '',
  department: '',
  status: 'ACTIVE',
};

const statusColor = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  COMPLETED: 'info',
  CANCELLED: 'error',
};

export default function ManageCourses() {
  const { enqueueSnackbar } = useSnackbar();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      setCourses(await courseApi.list());
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

  const openCreateDialog = () => {
    setEditingId(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEditDialog = (course) => {
    setEditingId(course.id);
    setForm({ ...emptyForm, ...course });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const payload = { ...form, credits: Number(form.credits), capacity: Number(form.capacity) };
      if (editingId) {
        await courseApi.update(editingId, payload);
        enqueueSnackbar('Course updated', { variant: 'success' });
      } else {
        await courseApi.create(payload);
        enqueueSnackbar('Course created', { variant: 'success' });
      }
      setDialogOpen(false);
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    try {
      await courseApi.remove(confirmDeleteId);
      enqueueSnackbar('Course deleted', { variant: 'success' });
      setConfirmDeleteId(null);
      load();
    } catch (err) {
      enqueueSnackbar(extractErrorMessage(err), { variant: 'error' });
    }
  };

  if (loading) return <LoadingSpinner label="Loading courses..." />;

  return (
    <Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h5">Courses</Typography>
          <Typography variant="body2" color="text.secondary">
            {courses.length} total
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Add Course
        </Button>
      </Stack>

      <Card component={Paper}>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Title</TableCell>
                <TableCell>Credits</TableCell>
                <TableCell>Capacity</TableCell>
                <TableCell>Semester</TableCell>
                <TableCell>Instructor</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {courses.map((c) => (
                <TableRow key={c.id} hover>
                  <TableCell>{c.courseCode}</TableCell>
                  <TableCell>{c.title}</TableCell>
                  <TableCell>{c.credits}</TableCell>
                  <TableCell>{c.capacity}</TableCell>
                  <TableCell>{c.semester || '—'}</TableCell>
                  <TableCell>{c.instructor || '—'}</TableCell>
                  <TableCell>
                    <Chip size="small" label={c.status} color={statusColor[c.status] || 'default'} variant="outlined" />
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => openEditDialog(c)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="error" onClick={() => setConfirmDeleteId(c.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {courses.length === 0 && (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No courses yet.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingId ? 'Edit Course' : 'Add Course'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Course Code"
                fullWidth
                value={form.courseCode}
                onChange={(e) => setForm((f) => ({ ...f, courseCode: e.target.value }))}
              />
              <TextField
                label="Credits"
                type="number"
                fullWidth
                value={form.credits}
                onChange={(e) => setForm((f) => ({ ...f, credits: e.target.value }))}
              />
            </Stack>
            <TextField
              label="Title"
              fullWidth
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            />
            <TextField
              label="Description"
              fullWidth
              multiline
              minRows={2}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="Capacity"
                type="number"
                fullWidth
                value={form.capacity}
                onChange={(e) => setForm((f) => ({ ...f, capacity: e.target.value }))}
              />
              <TextField
                label="Semester"
                fullWidth
                placeholder="FALL2026"
                value={form.semester}
                onChange={(e) => setForm((f) => ({ ...f, semester: e.target.value }))}
              />
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="Instructor"
                fullWidth
                value={form.instructor}
                onChange={(e) => setForm((f) => ({ ...f, instructor: e.target.value }))}
              />
              <TextField
                label="Department"
                fullWidth
                value={form.department}
                onChange={(e) => setForm((f) => ({ ...f, department: e.target.value }))}
              />
            </Stack>
            <TextField
              select
              label="Status"
              fullWidth
              value={form.status}
              onChange={(e) => setForm((f) => ({ ...f, status: e.target.value }))}
            >
              {Object.values(COURSE_STATUS).map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDialogOpen(false)} color="inherit">
            Cancel
          </Button>
          <Button onClick={handleSave} variant="contained" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!confirmDeleteId}
        title="Delete course?"
        message="This will permanently remove the course. Existing enrollments referencing it may become inconsistent."
        confirmLabel="Delete"
        destructive
        onConfirm={handleDelete}
        onClose={() => setConfirmDeleteId(null)}
      />
    </Box>
  );
}
