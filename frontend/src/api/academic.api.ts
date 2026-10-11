import { request } from './auth.api';
import type {
  AcademicMetadata,
  ApiResponse,
  ResponsibilitySummary,
  StudentDto,
  TimetableEntryDto,
} from '../types/auth.types';

export const academicApi = {
  async getMetadata(): Promise<AcademicMetadata> {
    const res = await request<ApiResponse<AcademicMetadata>>('/api/v1/academic/metadata', {
      method: 'GET',
    });
    return res.data;
  },

  async getSectionStudents(sectionId: string): Promise<StudentDto[]> {
    const res = await request<ApiResponse<StudentDto[]>>(
      `/api/v1/sections/${sectionId}/students`,
      { method: 'GET' }
    );
    return res.data;
  },

  async getSectionTimetable(sectionId: string): Promise<TimetableEntryDto[]> {
    const res = await request<ApiResponse<TimetableEntryDto[]>>(
      `/api/v1/sections/${sectionId}/timetable`,
      { method: 'GET' }
    );
    return res.data;
  },

  async getMyTeaching(): Promise<ResponsibilitySummary[]> {
    const res = await request<ApiResponse<ResponsibilitySummary[]>>(
      '/api/v1/faculty/my-teaching',
      { method: 'GET' }
    );
    return res.data;
  },

  async getAcademicPeriods(): Promise<import('../types/auth.types').PeriodDefinitionDto[]> {
    const res = await request<ApiResponse<import('../types/auth.types').PeriodDefinitionDto[]>>(
      '/api/v1/academic/periods',
      { method: 'GET' }
    );
    return res.data;
  },

  async getSectionOverview(sectionId: string): Promise<import('../types/auth.types').SectionDetailOverviewDto> {
    const res = await request<ApiResponse<import('../types/auth.types').SectionDetailOverviewDto>>(
      `/api/v1/sections/${sectionId}/overview`,
      { method: 'GET' }
    );
    return res.data;
  },
};
