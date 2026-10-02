import { request } from './auth.api';
import type { ApiResponse, ResponsibilityRequest, ResponsibilitySummary } from '../types/auth.types';

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
};
