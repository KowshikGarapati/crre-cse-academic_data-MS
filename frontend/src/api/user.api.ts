import { request } from './auth.api';
import type { ApiResponse, CreateUserRequest, UpdateUserRequest, UserSummary } from '../types/auth.types';

export const userApi = {
  async getAllUsers(role?: string): Promise<UserSummary[]> {
    const query = role && role !== 'ALL' ? `?role=${encodeURIComponent(role)}` : '';
    const res = await request<ApiResponse<UserSummary[]>>(`/api/v1/users${query}`, {
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

  async createStudentUser(payload: import('../types/auth.types').CreateStudentUserRequest): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>('/api/v1/users/students', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return res.data;
  },

  async createFacultyUser(payload: import('../types/auth.types').CreateFacultyUserRequest): Promise<UserSummary> {
    const res = await request<ApiResponse<UserSummary>>('/api/v1/users/faculty', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return res.data;
  },
};
