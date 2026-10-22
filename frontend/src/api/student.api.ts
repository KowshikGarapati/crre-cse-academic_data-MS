import { request } from './auth.api';
import type {
  ApiResponse,
  ReassignSectionRequest,
  ReassignSectionResponse,
  StudentCertificateDto,
  StudentCounsellingSummaryDto,
  StudentDetailDto,
  StudentDto,
  StudentFullProfileDto,
  TimetableEntryDto,
  UpdateStudentRequest,
  UpsertCertificateRequest,
} from '../types/auth.types';

export const studentApi = {
  async getScopedStudents(sectionId?: string): Promise<StudentDto[]> {
    const url = sectionId ? `/api/v1/students?sectionId=${encodeURIComponent(sectionId)}` : '/api/v1/students';
    const res = await request<ApiResponse<StudentDto[]>>(url, {
      method: 'GET',
    });
    return res.data;
  },

  async getMyProfile(): Promise<StudentFullProfileDto> {
    const res = await request<ApiResponse<StudentFullProfileDto>>('/api/v1/student/me', {
      method: 'GET',
    });
    const d = res.data;
    if (d && d.placement) {
      d.departmentName = d.departmentName || d.placement.departmentName;
      d.programName = d.programName || d.placement.programName;
      d.academicYearName = d.academicYearName || d.placement.academicYearName;
      d.semesterName = d.semesterName || d.placement.semesterName;
      d.sectionName = d.sectionName || d.placement.sectionName;
    }
    return d;
  },

  async getMyTimetable(): Promise<TimetableEntryDto[]> {
    const res = await request<ApiResponse<TimetableEntryDto[]>>('/api/v1/student/my-timetable', {
      method: 'GET',
    });
    return res.data;
  },

  async getMyCounselling(): Promise<StudentCounsellingSummaryDto> {
    const res = await request<ApiResponse<StudentCounsellingSummaryDto>>('/api/v1/student/my-counselling', {
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

  async updateStudent(studentId: string, req: UpdateStudentRequest): Promise<StudentDetailDto> {
    const res = await request<ApiResponse<StudentDetailDto>>(`/api/v1/students/${studentId}`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
    return res.data;
  },

  async getStudentCertificates(studentId: string): Promise<StudentCertificateDto[]> {
    const res = await request<ApiResponse<StudentCertificateDto[]>>(`/api/v1/students/${studentId}/certificates`, {
      method: 'GET',
    });
    return res.data;
  },

  async addStudentCertificate(studentId: string, req: UpsertCertificateRequest): Promise<StudentCertificateDto> {
    const res = await request<ApiResponse<StudentCertificateDto>>(`/api/v1/students/${studentId}/certificates`, {
      method: 'POST',
      body: JSON.stringify(req),
    });
    return res.data;
  },

  async updateStudentCertificate(studentId: string, certificateId: string, req: UpsertCertificateRequest): Promise<StudentCertificateDto> {
    const res = await request<ApiResponse<StudentCertificateDto>>(`/api/v1/students/${studentId}/certificates/${certificateId}`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
    return res.data;
  },

  async deleteStudentCertificate(studentId: string, certificateId: string): Promise<void> {
    await request<ApiResponse<void>>(`/api/v1/students/${studentId}/certificates/${certificateId}`, {
      method: 'DELETE',
    });
  },

  async reassignLateralSection(studentId: string, req: ReassignSectionRequest): Promise<ReassignSectionResponse> {
    const res = await request<ApiResponse<ReassignSectionResponse>>(`/api/v1/students/${studentId}/reassign-section`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
    return res.data;
  },
};
