export type Role = 'STUDENT' | 'FACULTY' | 'COUNSELLOR' | 'HOD' | 'ADMIN';

export interface UserSummary {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  registrationNumber?: string | null;
  uniqueId?: string | null;
  aadharNumber?: number | null;
  apaarNumber?: number | null;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiError {
  success: boolean;
  message: string;
  status: number;
  errors?: Record<string, string>;
  timestamp: string;
}
