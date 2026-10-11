import { request } from './auth.api';
import type {
  ApiResponse,
  CreateResourceRequest,
  ResourceDto,
  SubjectCurriculumDto,
} from '../types/auth.types';

export const curriculumApi = {
  async getSubjectCurriculum(subjectId: string): Promise<SubjectCurriculumDto> {
    const res = await request<ApiResponse<SubjectCurriculumDto>>(
      `/api/v1/curriculum/subjects/${subjectId}/units`,
      { method: 'GET' }
    );
    return res.data;
  },

  async getStudentResources(): Promise<SubjectCurriculumDto[]> {
    const res = await request<ApiResponse<SubjectCurriculumDto[]>>(
      '/api/v1/student/resources',
      { method: 'GET' }
    );
    return res.data;
  },

  async addResource(
    conceptId: string,
    data: CreateResourceRequest
  ): Promise<ResourceDto> {
    const res = await request<ApiResponse<ResourceDto>>(
      `/api/v1/curriculum/concepts/${conceptId}/resources`,
      {
        method: 'POST',
        body: JSON.stringify(data),
      }
    );
    return res.data;
  },
};
