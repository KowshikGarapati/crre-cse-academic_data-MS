import { request } from './auth.api';
import type {
  ApiResponse,
  DepartmentHierarchyDto,
  FacultyProfileDto,
  SubjectDto,
} from '../types/auth.types';

export const hodApi = {
  async getMyDepartmentHierarchy(): Promise<DepartmentHierarchyDto> {
    const res = await request<ApiResponse<DepartmentHierarchyDto>>(
      '/api/v1/hod/my-department',
      { method: 'GET' }
    );
    return res.data;
  },

  async getDepartmentFaculty(): Promise<FacultyProfileDto[]> {
    const res = await request<ApiResponse<FacultyProfileDto[]>>(
      '/api/v1/hod/faculty',
      { method: 'GET' }
    );
    return res.data;
  },

  async getDepartmentSubjects(): Promise<SubjectDto[]> {
    const res = await request<ApiResponse<SubjectDto[]>>(
      '/api/v1/hod/subjects',
      { method: 'GET' }
    );
    return res.data;
  },
};
