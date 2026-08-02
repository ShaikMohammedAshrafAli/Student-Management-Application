import { apiClient } from './axiosClient';

export const courseApi = {
  list: () => apiClient.get('/courses').then((r) => r.data),
  getById: (id) => apiClient.get(`/courses/${id}`).then((r) => r.data),
  create: (payload) => apiClient.post('/courses', payload).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/courses/${id}`, payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/courses/${id}`),
};
