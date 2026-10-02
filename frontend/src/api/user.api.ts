import { request } from './auth.api';
import type { ApiResponse, CreateUserRequest, UpdateUserRequest, UserSummary } from '../types/auth.types';

export const userApi = {
  async getAllUsers(): Promise<UserSummary[]> {
    const res = await request<ApiResponse<UserSummary[]>>('/api/v1/users', {
      method: 'GET',
    });
    return res.data;
  },

  async createUser(payload: CreateUserRequest): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>('/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return res.data;
  },

  async updateUser(id: string, payload: UpdateUserRequest): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>(`/api/v1/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
    return res.data;
  },

  async toggleUserStatus(id: string, enabled: boolean): Promise<void> {
    await request<ApiResponse<void>>(`/api/v1/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled }),
    });
  },
};
