import { Box, CircularProgress, Typography } from '@mui/material';

export default function LoadingSpinner({ label = 'Loading...', minHeight = 240 }) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        minHeight,
      }}
    >
      <CircularProgress size={36} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Box>
  );
}
