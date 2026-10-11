import { request } from './auth.api';
import type {
  ApiResponse,
  CounsellingRecordDto,
  CounsellingStudentDto,
  CreateCounsellingRecordRequest,
  FacultyProfileDto,
  FacultyTimetableEntryDto,
  MyClassDto,
  MySectionOverviewDto,
  MySubjectDto,
  SectionStudentsGroupDto,
  StudentDto,
} from '../types/auth.types';

export const facultyApi = {
  async getMySubjects(): Promise<MySubjectDto[]> {
    const res = await request<ApiResponse<MySubjectDto[]>>('/api/v1/faculty/my-subjects', {
      method: 'GET',
    });
    return res.data;
  },

  async getMyClasses(): Promise<MyClassDto[]> {
    const res = await request<ApiResponse<MyClassDto[]>>('/api/v1/faculty/my-classes', {
      method: 'GET',
    });
    return res.data;
  },

  async getMyTimetable(): Promise<FacultyTimetableEntryDto[]> {
    const res = await request<ApiResponse<FacultyTimetableEntryDto[]>>('/api/v1/faculty/my-timetable', {
      method: 'GET',
    });
    return res.data;
  },

  async getTodayClasses(day?: string): Promise<FacultyTimetableEntryDto[]> {
    const url = day ? `/api/v1/faculty/today-classes?day=${encodeURIComponent(day)}` : '/api/v1/faculty/today-classes';
    const res = await request<ApiResponse<FacultyTimetableEntryDto[]>>(url, {
      method: 'GET',
    });
    return res.data;
  },

  async getStudentsForClass(sectionId: string): Promise<StudentDto[]> {
    const res = await request<ApiResponse<StudentDto[]>>(
      `/api/v1/faculty/classes/${sectionId}/students`,
      { method: 'GET' }
    );
    return res.data;
  },

  async getMyStudentsGrouped(): Promise<SectionStudentsGroupDto[]> {
    const res = await request<ApiResponse<SectionStudentsGroupDto[]>>(
      '/api/v1/faculty/my-students/grouped',
      { method: 'GET' }
    );
    return res.data;
  },

  async getMySectionOverview(): Promise<MySectionOverviewDto | null> {
    const res = await request<ApiResponse<MySectionOverviewDto>>(
      '/api/v1/faculty/my-section/overview',
      { method: 'GET' }
    );
    return res.data;
  },

  async getMySectionStudents(): Promise<StudentDto[]> {
    const res = await request<ApiResponse<StudentDto[]>>(
      '/api/v1/faculty/my-section/students',
      { method: 'GET' }
    );
    return res.data;
  },

  async getMyCounsellingStudents(): Promise<CounsellingStudentDto[]> {
    const res = await request<ApiResponse<CounsellingStudentDto[]>>(
      '/api/v1/faculty/my-counselling/students',
      { method: 'GET' }
    );
    return res.data;
  },

  async getMyCounsellingRecords(): Promise<CounsellingRecordDto[]> {
    const res = await request<ApiResponse<CounsellingRecordDto[]>>(
      '/api/v1/faculty/my-counselling/records',
      { method: 'GET' }
    );
    return res.data;
  },

  async createCounsellingRecord(
    req: CreateCounsellingRecordRequest
  ): Promise<CounsellingRecordDto> {
    const res = await request<ApiResponse<CounsellingRecordDto>>(
      '/api/v1/faculty/my-counselling/records',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
    return res.data;
  },

  async getProfile(): Promise<FacultyProfileDto | null> {
    const res = await request<ApiResponse<FacultyProfileDto>>('/api/v1/faculty/profile', {
      method: 'GET',
    });
    return res.data;
  },
};
