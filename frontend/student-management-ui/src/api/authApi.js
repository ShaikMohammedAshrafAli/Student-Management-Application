import { apiClient } from './axiosClient';

export const authApi = {
  register: (payload) => apiClient.post('/auth/register', payload).then((r) => r.data.data),
  login: (payload) => apiClient.post('/auth/login', payload).then((r) => r.data.data),
  logout: (refreshToken) => apiClient.post('/auth/logout', { refreshToken }).then((r) => r.data),
};

export const adminApi = {
  listUsers: () => apiClient.get('/admin/users').then((r) => r.data.data),
  assignRole: (userId, role) => apiClient.patch(`/admin/users/${userId}/role`, { role }).then((r) => r.data.data),
  setEnabled: (userId, enabled) => apiClient.patch(`/admin/users/${userId}/status`, { enabled }).then((r) => r.data.data),
};
