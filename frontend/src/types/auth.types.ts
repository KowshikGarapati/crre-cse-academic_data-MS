export type Role =
  | 'PRINCIPAL'
  | 'HOD'
  | 'FACULTY'
  | 'COUNSELLOR'
  | 'CLASS_TEACHER'
  | 'STUDENT'
  | 'ADMIN';

export interface ResponsibilitySummary {
  id: string;
  type: string;
  departmentId?: string | null;
  departmentName?: string | null;
  programId?: string | null;
  programName?: string | null;
  subjectId?: string | null;
  subjectCode?: string | null;
  subjectName?: string | null;
  sectionId?: string | null;
  sectionName?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  description?: string | null;
}

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
  enabled: boolean;
  responsibilities: ResponsibilitySummary[];
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthResponse {
  message: string;
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

export interface DepartmentDto {
  id: string;
  code: string;
  name: string;
}

export interface ProgramDto {
  id: string;
  departmentId: string;
  code: string;
  name: string;
  degree: string;
}

export interface SectionDto {
  id: string;
  semesterId: string;
  sectionNumber: number;
  sectionName: string;
}

export interface SubjectDto {
  id: string;
  programId: string;
  semesterId: string;
  code: string;
  name: string;
  type: string;
  credits: number;
}

export interface AcademicMetadata {
  departments: DepartmentDto[];
  programs: ProgramDto[];
  sections: SectionDto[];
  subjects: SubjectDto[];
}

export interface StudentDto {
  id: string;
  registrationNumber: string;
  fullName: string;
  entryType: string;
  status: string;
  sectionId: string;
  sectionName: string;
  programName: string;
  semesterName: string;
}

export interface TimetableEntryDto {
  id: string;
  sectionId: string;
  sectionName: string;
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  facultyName: string;
  dayOfWeek: string;
  periodNumber: number;
  startTime: string;
  endTime: string;
}

export interface CreateUserRequest {
  fullName: string;
  username: string;
  email: string;
  password: string;
  role: Role;
  registrationNumber?: string;
  uniqueId?: string;
  aadharNumber?: number;
  apaarNumber?: number;
  departmentId?: string;
  programId?: string;
  subjectId?: string;
  sectionId?: string;
  classTeacher?: boolean;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  password?: string;
  registrationNumber?: string;
  uniqueId?: string;
  aadharNumber?: number;
  apaarNumber?: number;
}

export interface ResponsibilityRequest {
  type: string;
  departmentId?: string;
  programId?: string;
  subjectId?: string;
  sectionId?: string;
  description?: string;
}
