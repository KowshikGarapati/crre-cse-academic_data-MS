import { request } from './auth.api';
import type {
  ApiResponse,
  CollegeOverviewDto,
  DepartmentHierarchyDto,
  StudentDto,
} from '../types/auth.types';

export const principalApi = {
  async getCollegeOverview(): Promise<CollegeOverviewDto> {
    const res = await request<ApiResponse<CollegeOverviewDto>>(
      '/api/v1/principal/college-overview',
      { method: 'GET' }
    );
    return res.data;
  },

  async getDepartmentDetails(deptId: string): Promise<DepartmentHierarchyDto> {
    const res = await request<ApiResponse<DepartmentHierarchyDto>>(
      `/api/v1/principal/departments/${deptId}`,
      { method: 'GET' }
    );
    return res.data;
  },

  async getAllStudents(): Promise<StudentDto[]> {
    const res = await request<ApiResponse<StudentDto[]>>(
      '/api/v1/principal/students',
      { method: 'GET' }
    );
    return res.data;
  },
};
