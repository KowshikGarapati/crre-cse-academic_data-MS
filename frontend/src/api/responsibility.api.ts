import { request } from './auth.api';
import type { ApiResponse, CounsellorStudentDto, ResponsibilityRequest, ResponsibilitySummary } from '../types/auth.types';

export const responsibilityApi = {
  async getUserResponsibilities(userId: string): Promise<ResponsibilitySummary[]> {
    const res = await request<ApiResponse<ResponsibilitySummary[]>>(
      `/api/v1/users/${userId}/responsibilities`,
      { method: 'GET' }
    );
    return res.data;
  },

  async addResponsibility(
    userId: string,
    payload: ResponsibilityRequest
  ): Promise<ResponsibilitySummary> {
    const res = await request<ApiResponse<ResponsibilitySummary>>(
      `/api/v1/users/${userId}/responsibilities`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
    return res.data;
  },

  async updateResponsibility(
    id: string,
    payload: ResponsibilityRequest
  ): Promise<ResponsibilitySummary> {
    const res = await request<ApiResponse<ResponsibilitySummary>>(
      `/api/v1/responsibilities/${id}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload),
      }
    );
    return res.data;
  },

  async removeResponsibility(id: string): Promise<void> {
    await request<ApiResponse<void>>(`/api/v1/responsibilities/${id}`, {
      method: 'DELETE',
    });
  },

  async getCounsellingStudents(responsibilityId: string): Promise<CounsellorStudentDto[]> {
    const res = await request<ApiResponse<CounsellorStudentDto[]>>(
      `/api/v1/responsibilities/${responsibilityId}/counselling-students`,
      { method: 'GET' }
    );
    return res.data;
  },

  async assignCounsellingStudents(
    responsibilityId: string,
    studentIds: string[]
  ): Promise<CounsellorStudentDto[]> {
    const res = await request<ApiResponse<CounsellorStudentDto[]>>(
      `/api/v1/responsibilities/${responsibilityId}/counselling-students`,
      {
        method: 'POST',
        body: JSON.stringify({ studentIds }),
      }
    );
    return res.data;
  },

  async removeCounsellingStudent(
    responsibilityId: string,
    studentId: string
  ): Promise<void> {
    await request<ApiResponse<void>>(
      `/api/v1/responsibilities/${responsibilityId}/counselling-students/${studentId}`,
      { method: 'DELETE' }
    );
  },
};
