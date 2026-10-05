import { request } from './auth.api';
import type {
  ApiResponse,
  CounsellingRecordDto,
  StudentDetailDto,
  StudentFullProfileDto,
  TimetableEntryDto,
} from '../types/auth.types';

export const studentApi = {
  async getMyProfile(): Promise<StudentFullProfileDto> {
    const res = await request<ApiResponse<StudentFullProfileDto>>('/api/v1/student/me', {
      method: 'GET',
    });
    return res.data;
  },

  async getMyTimetable(): Promise<TimetableEntryDto[]> {
    const res = await request<ApiResponse<TimetableEntryDto[]>>('/api/v1/student/my-timetable', {
      method: 'GET',
    });
    return res.data;
  },

  async getMyCounselling(): Promise<CounsellingRecordDto[]> {
    const res = await request<ApiResponse<CounsellingRecordDto[]>>('/api/v1/student/my-counselling', {
      method: 'GET',
    });
    return res.data;
  },

  async getStudentDetails(studentId: string): Promise<StudentDetailDto> {
    const res = await request<ApiResponse<StudentDetailDto>>(`/api/v1/students/${studentId}`, {
      method: 'GET',
    });
    return res.data;
  },
};
