import { apiClient } from './axiosClient';

export const enrollmentApi = {
  enroll: (studentId, courseId) => apiClient.post('/enrollments', { studentId, courseId }).then((r) => r.data),
  getById: (id) => apiClient.get(`/enrollments/${id}`).then((r) => r.data),
  getByStudent: (studentId) => apiClient.get(`/enrollments/student/${studentId}`).then((r) => r.data),
  getByCourse: (courseId) => apiClient.get(`/enrollments/course/${courseId}`).then((r) => r.data),
  updateStatus: (id, status) => apiClient.patch(`/enrollments/${id}/status`, { status }).then((r) => r.data),
  drop: (id) => apiClient.delete(`/enrollments/${id}`),
};
