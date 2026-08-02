import { apiClient } from './axiosClient';

export const studentApi = {
  getById: (id) => apiClient.get(`/students/${id}`).then((r) => r.data),
  list: (params = {}) => apiClient.get('/students', { params: { unpaged: true, ...params } }).then((r) => r.data),
  create: (payload) => apiClient.post('/students', payload).then((r) => r.data),
  update: (id, payload) => apiClient.put(`/students/${id}`, payload).then((r) => r.data),
  remove: (id) => apiClient.delete(`/students/${id}`),
};
