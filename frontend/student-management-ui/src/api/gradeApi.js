import { apiClient } from './axiosClient';

// Note: grade-service (like student/course/enrollment-service) returns raw
// DTOs, not wrapped in the common-lib ApiResponse envelope - only
// auth-service does that. Hence `.data` here, not `.data.data`.
export const gradeApi = {
  assign: (enrollmentId, gradePoints) =>
    apiClient.post('/grades', { enrollmentId, gradePoints }).then((r) => r.data),
  getByStudent: (studentId) => apiClient.get(`/grades/student/${studentId}`).then((r) => r.data),
  getGpa: (studentId) => apiClient.get(`/grades/student/${studentId}/gpa`).then((r) => r.data),
};
