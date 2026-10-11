import { request } from './auth.api';
import type {
  ApiResponse,
  CounsellingRecordDto,
  CounsellingStudentDto,
  CreateCounsellingRecordRequest,
} from '../types/auth.types';

export const counsellingApi = {
  async getMentees(): Promise<CounsellingStudentDto[]> {
    const res = await request<ApiResponse<CounsellingStudentDto[]>>(
      '/api/v1/counselling/mentees',
      { method: 'GET' }
    );
    return res.data;
  },

  async getRecords(): Promise<CounsellingRecordDto[]> {
    const res = await request<ApiResponse<CounsellingRecordDto[]>>(
      '/api/v1/counselling/records',
      { method: 'GET' }
    );
    return res.data;
  },

  async createRecord(
    req: CreateCounsellingRecordRequest
  ): Promise<CounsellingRecordDto> {
    const res = await request<ApiResponse<CounsellingRecordDto>>(
      '/api/v1/counselling/records',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
    return res.data;
  },
};
