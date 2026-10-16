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
  programCode?: string | null;
  programName?: string | null;
  subjectId?: string | null;
  subjectCode?: string | null;
  subjectName?: string | null;
  sectionId?: string | null;
  sectionName?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  description?: string | null;
  userId?: string | null;
  assignedStudentCount?: number | null;
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
  mustChangePassword?: boolean;
  responsibilities: ResponsibilitySummary[];
}

export const isUserHod = (user: UserSummary | null | undefined): boolean => {
  if (!user) return false;
  return user.role === 'HOD' || (user.responsibilities?.some(r => r.type === 'HOD_DEPT' && r.status === 'ACTIVE') ?? false);
};

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
  programCode?: string;
  programName?: string;
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

export interface SemesterDto {
  id: string;
  programId: string;
  semesterNumber: number;
  termName: string;
  current: boolean;
}

export interface AcademicMetadata {
  departments: DepartmentDto[];
  programs: ProgramDto[];
  semesters?: SemesterDto[];
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
  roomNumber?: string;
  sessionType?: string;
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
  studentIds?: string[];
}

export interface CounsellorStudentDto {
  assignmentId: string;
  studentId: string;
  registrationNumber: string;
  fullName: string;
  entryType: string;
  sectionName: string;
  programName: string;
  semesterName: string;
  assignedAt: string;
  status: string;
}

export interface UpdateStudentRequest {
  fullName: string;
  email?: string;
  entryType?: 'REGULAR' | 'LATERAL';
  admissionCategory?: 'CONVENOR' | 'MANAGEMENT' | '' | null;
  tuitionCategory?: 'RTF' | 'SELF_FINANCE' | '' | null;
  aadharNumber?: number | null;
  apaarNumber?: number | null;
  sectionId?: string | null;
  status?: 'ACTIVE' | 'INACTIVE';
}

// ==================== FACULTY DTOS ====================
export interface MySubjectDto {
  subjectId: string;
  code: string;
  name: string;
  type: string;
  credits: number;
  sectionId: string;
  sectionName: string;
  programName: string;
  semesterName: string;
}

export interface MyClassDto {
  sectionId: string;
  sectionName: string;
  programCode?: string | null;
  programName: string;
  semesterName: string;
  academicYearName: string;
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  studentCount: number;
}

export interface FacultyTimetableEntryDto {
  id: string;
  dayOfWeek: string;
  periodNumber: number;
  startTime: string;
  endTime: string;
  subjectCode: string;
  subjectName: string;
  sectionName: string;
  semesterName: string;
  programName?: string;
  roomNumber?: string;
}

export interface MySectionOverviewDto {
  sectionId: string;
  sectionName: string;
  programName: string;
  semesterName: string;
  academicYearName: string;
  classTeacherName: string;
  totalStudents: number;
  regularStudents: number;
  lateralStudents: number;
}

export interface CounsellingStudentDto {
  studentId: string;
  registrationNumber: string;
  fullName: string;
  entryType: string;
  sectionName: string;
  programName: string;
  semesterName: string;
  totalSessions: number;
  lastMeetingDate?: string | null;
  lastStatus: string;
}

export interface CounsellingRecordDto {
  id: string;
  counsellorName: string;
  studentId: string;
  studentRegistrationNumber: string;
  studentName: string;
  meetingDate: string;
  recordType: string;
  remarks: string;
  actionPlan?: string;
  followUpDate?: string;
  status: string;
  createdAt: string;
}

export interface CreateCounsellingRecordRequest {
  studentId: string;
  meetingDate: string;
  recordType: string;
  remarks: string;
  actionPlan?: string;
  followUpDate?: string;
  status: string;
}

export interface FacultyProfileDto {
  id: string;
  employeeNumber: string;
  departmentName: string;
  designation: string;
  qualification?: string;
  specialization?: string;
  professionalExperience?: string;
  institutionalContact?: string;
  activeStatus: string;
}

// ==================== STUDENT DTOS ====================
export interface EducationalHistoryDto {
  id: string;
  qualificationType: string;
  boardOrUniversity: string;
  institutionName: string;
  hallTicketNumber?: string;
  completionYear: number;
  streamOrBranch?: string;
  gradeOrCgpa?: string;
  maxMarks?: number;
  marksObtained?: number;
  percentage?: number;
  remarks?: string;
}

export interface StudentFullProfileDto {
  id: string;
  registrationNumber: string;
  fullName: string;
  entryType: string;
  email: string;
  placement?: StudentPlacementDto;
  departmentName?: string;
  programName?: string;
  academicYearName?: string;
  semesterName?: string;
  sectionName?: string;
  admissionCategory?: string | null;
  tuitionCategory?: string | null;
  classTeacherName?: string;
  classTeacherEmail?: string;
  counsellorName?: string;
  counsellorEmail?: string;
  educationalHistory: EducationalHistoryDto[];
}

export interface StudentPlacementDto {
  departmentId?: string;
  departmentName?: string;
  programId?: string;
  programName?: string;
  degree?: string;
  academicYearId?: string;
  academicYearName?: string;
  yearNumber?: number;
  semesterId?: string;
  semesterName?: string;
  semesterNumber?: number;
  sectionId?: string;
  sectionName?: string;
}

export interface StudentCourseRegistrationDto {
  id: string;
  code: string;
  name: string;
  type: string;
  credits: number;
  facultyName?: string;
  facultyEmail?: string;
}

export interface StudentCounsellingSessionDto {
  id: string;
  meetingDate: string;
  recordType: string;
  remarks: string;
  actionPlan?: string;
  followUpDate?: string;
  status: string;
}

export interface StudentCounsellingSummaryDto {
  counsellorName: string;
  counsellorEmail?: string;
  counsellorEmployeeNumber?: string;
  sessions: StudentCounsellingSessionDto[];
}

export interface StudentDetailDto {
  id: string;
  userId?: string;
  registrationNumber: string;
  fullName: string;
  email?: string;
  uniqueId?: string;
  aadharNumber?: number;
  apaarNumber?: number;
  entryType: string;
  status: string;
  admissionCategory?: string | null;
  tuitionCategory?: string | null;
  placement?: StudentPlacementDto;
  classTeacherName?: string;
  classTeacherEmail?: string;
  counsellorName?: string;
  counsellorEmail?: string;
  educationalHistory: EducationalHistoryDto[];
  currentCourses: StudentCourseRegistrationDto[];
  timetable: TimetableEntryDto[];
  canViewCounselling: boolean;
  counsellingRecords: StudentCounsellingSessionDto[];
  attendanceStatus?: string;
  resultsStatus?: string;
}

// ==================== HOD DTOS ====================
export interface HodSectionDto {
  id?: string;
  sectionId: string;
  sectionName: string;
  sectionNumber: number;
  classTeacherName?: string;
  studentCount: number;
}

export interface HodSemesterDto {
  id?: string;
  semesterId: string;
  termName: string;
  semesterNumber: number;
  sections: HodSectionDto[];
}

export interface HodProgramDto {
  id?: string;
  programId: string;
  code?: string;
  programCode: string;
  name?: string;
  programName: string;
  degree: string;
  semesters: HodSemesterDto[];
}

export interface HodAcademicYearDto {
  id?: string;
  academicYearId: string;
  yearName: string;
  isCurrent?: boolean;
  current?: boolean;
  programs: HodProgramDto[];
}

export interface DepartmentHierarchyDto {
  departmentId: string;
  departmentCode: string;
  departmentName: string;
  hodName: string;
  totalFaculty: number;
  totalStudents: number;
  academicYears: HodAcademicYearDto[];
}

// ==================== PRINCIPAL DTOS ====================
export interface PrincipalProgramDto {
  programId: string;
  code: string;
  name: string;
  degree: string;
  studentCount: number;
}

export interface PrincipalDepartmentDto {
  id?: string;
  departmentId: string;
  code: string;
  name: string;
  hodName?: string;
  facultyCount: number;
  studentCount: number;
  programs: PrincipalProgramDto[];
}

export interface CollegeOverviewDto {
  institutionName: string;
  code: string;
  principalName: string;
  totalDepartments: number;
  totalPrograms: number;
  totalFaculty: number;
  totalStudents: number;
  departments: PrincipalDepartmentDto[];
}

// ==================== PERIOD & SECTION OVERVIEW DTOS ====================
export interface PeriodDefinitionDto {
  periodNumber: number;
  startTime: string;
  endTime: string;
  label: string;
  timeRange: string;
}

export interface EnrolledSubjectFacultyDto {
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  subjectType: string;
  credits: number;
  facultyId?: string | null;
  facultyName: string;
  facultyEmail?: string | null;
}

export interface SectionDetailOverviewDto {
  sectionId: string;
  sectionName: string;
  programId: string;
  programName: string;
  programCode: string;
  semesterId: string;
  semesterName: string;
  academicYearName: string;
  classTeacherId?: string | null;
  classTeacherName: string;
  classTeacherEmail?: string | null;
  totalStudents: number;
  regularStudents: number;
  lateralStudents: number;
  subjects: EnrolledSubjectFacultyDto[];
  timetable: TimetableEntryDto[];
  students: StudentDto[];
}

export interface SectionStudentsGroupDto {
  sectionId: string;
  sectionName: string;
  programName: string;
  semesterName: string;
  subjectCode: string;
  subjectName: string;
  students: StudentDto[];
}

// ==================== CREATION REQUESTS ====================
export interface CreateEducationalHistoryRequest {
  qualificationType: string;
  boardOrUniversity: string;
  institutionName: string;
  hallTicketNumber?: string;
  completionYear: number;
  streamOrBranch?: string;
  gradeOrCgpa?: string;
  maxMarks?: number;
  marksObtained?: number;
  percentage?: number;
  remarks?: string;
}

export interface CreateStudentUserRequest {
  username: string;
  email: string;
  fullName: string;
  password?: string;
  registrationNumber: string;
  uniqueId?: string;
  aadharNumber?: number;
  apaarNumber?: number;
  entryType: string;
  departmentId: string;
  programId: string;
  academicYearId: string;
  semesterId: string;
  sectionId: string;
  educationalHistory: CreateEducationalHistoryRequest[];
}

export interface TeachingAssignmentInput {
  subjectId: string;
  sectionId: string;
}

export interface CreateFacultyUserRequest {
  username: string;
  email: string;
  fullName: string;
  password?: string;
  employeeNumber: string;
  aadharNumber?: number;
  apaarNumber?: number;
  departmentId: string;
  designation: string;
  qualification?: string;
  specialization?: string;
  professionalExperience?: string;
  institutionalContact?: string;
  teachingAssignments?: TeachingAssignmentInput[];
  classTeacherSectionId?: string;
  counsellor?: boolean;
}

// ==================== CURRICULUM & RESOURCES ====================
export interface ResourceDto {
  id: string;
  conceptId: string;
  title: string;
  description?: string;
  resourceType: 'YOUTUBE_VIDEO' | 'PDF_DOCUMENT';
  url: string;
  fileSizeBytes?: number;
  uploadedByName: string;
  createdAt: string;
}

export interface ConceptDto {
  id: string;
  unitId: string;
  conceptOrder: number;
  title: string;
  description?: string;
  resources: ResourceDto[];
}

export interface UnitDto {
  id: string;
  subjectId: string;
  unitNumber: number;
  title: string;
  description?: string;
  concepts: ConceptDto[];
}

export interface SubjectCurriculumDto {
  subjectId: string;
  subjectCode: string;
  subjectName: string;
  type: string;
  credits: number;
  facultyInstructor?: string;
  units: UnitDto[];
}

export interface CreateResourceRequest {
  title: string;
  description?: string;
  resourceType: string;
  url: string;
  fileSizeBytes?: number;
}
