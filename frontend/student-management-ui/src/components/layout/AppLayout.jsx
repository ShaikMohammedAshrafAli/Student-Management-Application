import { useState } from 'react';
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/DashboardOutlined';
import PeopleIcon from '@mui/icons-material/PeopleOutlineOutlined';
import MenuBookIcon from '@mui/icons-material/MenuBookOutlined';
import AssignmentIcon from '@mui/icons-material/AssignmentOutlined';
import GradeIcon from '@mui/icons-material/GradeOutlined';
import GroupIcon from '@mui/icons-material/GroupOutlined';
import PersonIcon from '@mui/icons-material/PersonOutlineOutlined';
import LogoutIcon from '@mui/icons-material/LogoutOutlined';
import SchoolIcon from '@mui/icons-material/School';
import { useAuth } from '../../context/AuthContext';

const DRAWER_WIDTH = 248;

const ADMIN_NAV = [
  { label: 'Dashboard', path: '/admin', icon: <DashboardIcon /> },
  { label: 'Students', path: '/admin/students', icon: <PeopleIcon /> },
  { label: 'Courses', path: '/admin/courses', icon: <MenuBookIcon /> },
  { label: 'Enrollments', path: '/admin/enrollments', icon: <AssignmentIcon /> },
  { label: 'Grades', path: '/admin/grades', icon: <GradeIcon /> },
  { label: 'Users', path: '/admin/users', icon: <GroupIcon /> },
];

const STUDENT_NAV = [
  { label: 'Dashboard', path: '/student', icon: <DashboardIcon /> },
  { label: 'Available Courses', path: '/student/courses', icon: <MenuBookIcon /> },
  { label: 'My Courses', path: '/student/my-courses', icon: <AssignmentIcon /> },
  { label: 'My Grades', path: '/student/grades', icon: <GradeIcon /> },
  { label: 'Profile', path: '/student/profile', icon: <PersonIcon /> },
];

export default function AppLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);

  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = isAdmin ? ADMIN_NAV : STUDENT_NAV;

  const handleLogout = () => {
    setAnchorEl(null);
    logout();
    navigate('/login');
  };

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ gap: 1.2 }}>
        <SchoolIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={700} noWrap>
          StudentHub
        </Typography>
      </Toolbar>
      <Divider />
      <List sx={{ px: 1.5, py: 1.5, flexGrow: 1 }}>
        {navItems.map((item) => {
          const selected = location.pathname === item.path;
          return (
            <ListItemButton
              key={item.path}
              component={RouterLink}
              to={item.path}
              selected={selected}
              onClick={() => isMobile && setMobileOpen(false)}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                '&.Mui-selected': {
                  bgcolor: 'primary.main',
                  color: 'primary.contrastText',
                  '& .MuiListItemIcon-root': { color: 'primary.contrastText' },
                  '&:hover': { bgcolor: 'primary.dark' },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 38 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: 14, fontWeight: 600 }} />
            </ListItemButton>
          );
        })}
      </List>
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          Signed in as {isAdmin ? 'Administrator' : 'Student'}
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.paper',
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <IconButton
            edge="start"
            onClick={() => setMobileOpen(true)}
            sx={{ display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Box sx={{ flexGrow: 1 }} />
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ gap: 1, borderRadius: 2 }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main', fontSize: 14 }}>
              {(user?.fullName || user?.email || '?').charAt(0).toUpperCase()}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="body2" fontWeight={600}>
                {user?.fullName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.email}
              </Typography>
            </Box>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, borderRight: '1px solid', borderColor: 'divider' },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          bgcolor: 'background.default',
          minHeight: '100vh',
        }}
      >
        <Toolbar />
        <Box sx={{ p: { xs: 2, sm: 3 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
