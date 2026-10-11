import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { studentApi } from '../api/student.api';
import { facultyApi } from '../api/faculty.api';
import { hodApi } from '../api/hod.api';
import { principalApi } from '../api/principal.api';
import type {
  CollegeOverviewDto,
  CounsellingRecordDto,
  CounsellingStudentDto,
  DepartmentHierarchyDto,
  FacultyProfileDto,
  FacultyTimetableEntryDto,
  MyClassDto,
  MySectionOverviewDto,
  MySubjectDto,
  StudentFullProfileDto,
  SubjectDto,
  TimetableEntryDto,
} from '../types/auth.types';
import { isUserHod } from '../types/auth.types';
import {
  Building,
  GraduationCap,
  Users,
  Calendar,
  BookOpen,
  Award,
  Layers,
  HeartHandshake,
  Clock,
  CheckCircle2,
  FileText,
  PlusCircle,
  Loader2,
  ChevronRight,
  ArrowLeft,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Role-specific data states
  const [loading, setLoading] = useState(true);
  const [studentProfile, setStudentProfile] = useState<StudentFullProfileDto | null>(null);
  const [studentCounselling, setStudentCounselling] = useState<CounsellingRecordDto[]>([]);
  const [studentTimetable, setStudentTimetable] = useState<TimetableEntryDto[]>([]);

  const [facultySubjects, setFacultySubjects] = useState<MySubjectDto[]>([]);
  const [facultyClasses, setFacultyClasses] = useState<MyClassDto[]>([]);
  const [facultyTimetable, setFacultyTimetable] = useState<FacultyTimetableEntryDto[]>([]);
  const [todayClasses, setTodayClasses] = useState<FacultyTimetableEntryDto[]>([]);
  const [sectionOverview, setSectionOverview] = useState<MySectionOverviewDto | null>(null);
  const [counsellingStudents, setCounsellingStudents] = useState<CounsellingStudentDto[]>([]);
  const [counsellingRecords, setCounsellingRecords] = useState<CounsellingRecordDto[]>([]);

  const [todayWeekday] = useState(() =>
    new Intl.DateTimeFormat('en-US', { weekday: 'long' }).format(new Date())
  );

  const [hodHierarchy, setHodHierarchy] = useState<DepartmentHierarchyDto | null>(null);
  const [hodFaculty, setHodFaculty] = useState<FacultyProfileDto[]>([]);
  const [hodSubjects, setHodSubjects] = useState<SubjectDto[]>([]);

  const [collegeOverview, setCollegeOverview] = useState<CollegeOverviewDto | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedDeptId = searchParams.get('dept');
  const [selectedDeptHierarchy, setSelectedDeptHierarchy] = useState<DepartmentHierarchyDto | null>(null);
  const [loadingDept, setLoadingDept] = useState(false);

  // New Counselling record modal state
  const [showRecordModal, setShowRecordModal] = useState(false);
  const [selectedMentee, setSelectedMentee] = useState<string>('');
  const [recordType, setRecordType] = useState('GENERAL');
  const [meetingDate, setMeetingDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [remarks, setRemarks] = useState('');
  const [actionPlan, setActionPlan] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [submittingRecord, setSubmittingRecord] = useState(false);

  useEffect(() => {
    async function loadDashboardData() {
      if (!user) return;
      setLoading(true);
      try {
        if (user.role === 'STUDENT') {
          const [prof, counsSummary, tt] = await Promise.all([
            studentApi.getMyProfile().catch(() => null),
            studentApi.getMyCounselling().catch(() => null),
            studentApi.getMyTimetable().catch(() => []),
          ]);
          setStudentProfile(prof);
          setStudentTimetable(tt);
          if (counsSummary && counsSummary.sessions) {
            const mapped: CounsellingRecordDto[] = counsSummary.sessions.map((s) => ({
              id: s.id,
              counsellorName: counsSummary.counsellorName || prof?.counsellorName || 'Assigned Counsellor',
              studentId: prof?.id || '',
              studentRegistrationNumber: prof?.registrationNumber || '',
              studentName: prof?.fullName || '',
              meetingDate: s.meetingDate,
              recordType: s.recordType,
              remarks: s.remarks,
              actionPlan: s.actionPlan,
              followUpDate: s.followUpDate,
              status: s.status,
              createdAt: s.meetingDate,
            }));
            setStudentCounselling(mapped);
          } else {
            setStudentCounselling([]);
          }
        } else if (user.role === 'FACULTY') {
          const isCT = user.responsibilities?.some((r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE');
          const isCounsellor = user.responsibilities?.some((r) => r.type === 'COUNSELLOR' && r.status === 'ACTIVE');

          const [subs, cls, tt, todayCls] = await Promise.all([
            facultyApi.getMySubjects().catch(() => []),
            facultyApi.getMyClasses().catch(() => []),
            facultyApi.getMyTimetable().catch(() => []),
            facultyApi.getTodayClasses().catch(() => []),
          ]);
          setFacultySubjects(subs);
          setFacultyClasses(cls);
          setFacultyTimetable(tt);
          setTodayClasses(todayCls);

          if (isCT) {
            const overview = await facultyApi.getMySectionOverview().catch(() => null);
            setSectionOverview(overview);
          }
          if (isCounsellor) {
            const [cStudents, cRecords] = await Promise.all([
              facultyApi.getMyCounsellingStudents().catch(() => []),
              facultyApi.getMyCounsellingRecords().catch(() => []),
            ]);
            setCounsellingStudents(cStudents);
            setCounsellingRecords(cRecords);
            if (cStudents.length > 0) {
              setSelectedMentee(cStudents[0].studentId);
            }
          }
          if (isUserHod(user)) {
            const [hier, fac, subs] = await Promise.all([
              hodApi.getMyDepartmentHierarchy().catch(() => null),
              hodApi.getDepartmentFaculty().catch(() => []),
              hodApi.getDepartmentSubjects().catch(() => []),
            ]);
            setHodHierarchy(hier);
            setHodFaculty(fac);
            setHodSubjects(subs);
          }
        } else if (user.role === 'HOD') {
          const [hier, fac, subs] = await Promise.all([
            hodApi.getMyDepartmentHierarchy().catch(() => null),
            hodApi.getDepartmentFaculty().catch(() => []),
            hodApi.getDepartmentSubjects().catch(() => []),
          ]);
          setHodHierarchy(hier);
          setHodFaculty(fac);
          setHodSubjects(subs);
        } else if (user.role === 'PRINCIPAL') {
          const overview = await principalApi.getCollegeOverview().catch(() => null);
          setCollegeOverview(overview);
        }
      } catch (err) {
        console.error('Failed to load role dashboard data', err);
      } finally {
        setLoading(false);
      }
    }

    loadDashboardData();
  }, [user]);

  useEffect(() => {
    if (user?.role === 'PRINCIPAL' && selectedDeptId) {
      setLoadingDept(true);
      principalApi
        .getDepartmentDetails(selectedDeptId)
        .then((data) => setSelectedDeptHierarchy(data))
        .catch((err) => {
          console.error('Failed to load department hierarchy for Principal', err);
          setSelectedDeptHierarchy(null);
        })
        .finally(() => setLoadingDept(false));
    } else {
      setSelectedDeptHierarchy(null);
    }
  }, [user, selectedDeptId]);

  const handleCreateCounsellingRecord = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMentee || !remarks.trim()) return;
    setSubmittingRecord(true);
    try {
      const created = await facultyApi.createCounsellingRecord({
        studentId: selectedMentee,
        meetingDate,
        recordType,
        remarks: remarks.trim(),
        actionPlan: actionPlan.trim() || undefined,
        followUpDate: followUpDate || undefined,
        status: 'OPEN',
      });
      setCounsellingRecords((prev) => [created, ...prev]);
      setShowRecordModal(false);
      setRemarks('');
      setActionPlan('');
      setFollowUpDate('');
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to create record');
    } finally {
      setSubmittingRecord(false);
    }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Dynamic Context Header */}
        <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-2xs mb-6 text-xs text-slate-600 flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1 font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
            <Building className="w-3.5 h-3.5" /> CRRE
          </span>
          <span className="text-slate-400">/</span>
          {user.role === 'PRINCIPAL' || user.role === 'ADMIN' ? (
            <span className="font-semibold text-slate-800">College Administration • Institutional Scope</span>
          ) : user.role === 'HOD' || isUserHod(user) ? (
            <>
              <span className="font-semibold text-slate-800">
                {hodHierarchy?.departmentName || 'Computer Science & Engineering (CSE)'}
              </span>
              <span className="text-slate-400">/</span>
              <span className="font-semibold text-indigo-900">Departmental Scope</span>
            </>
          ) : user.role === 'STUDENT' ? (
            <>
              <span className="font-semibold text-slate-800">
                {studentProfile?.departmentName || 'CSE Department'}
              </span>
              <span className="text-slate-400">/</span>
              <span className="font-semibold text-indigo-900">
                {studentProfile?.programName || 'Degree Program'}
              </span>
              <span className="text-slate-400">/</span>
              <span className="text-slate-700">
                {studentProfile?.semesterName || 'Semester'} {studentProfile?.academicYearName ? `(${studentProfile.academicYearName})` : ''}
              </span>
              <span className="text-slate-400">/</span>
              <span className="inline-flex items-center gap-1 font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                {studentProfile?.sectionName || 'Section'}
              </span>
            </>
          ) : sectionOverview ? (
            <>
              <span className="font-semibold text-slate-800">
                Computer Science & Engineering (CSE)
              </span>
              <span className="text-slate-400">/</span>
              <span className="font-semibold text-indigo-900">
                {sectionOverview.programName}
              </span>
              <span className="text-slate-400">/</span>
              <span className="text-slate-700">
                {sectionOverview.semesterName}
              </span>
              <span className="text-slate-400">/</span>
              <span className="inline-flex items-center gap-1 font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                {sectionOverview.sectionName}
              </span>
            </>
          ) : (
            <span className="font-semibold text-slate-800">
              Computer Science & Engineering (CSE) • Faculty Academic Scope
            </span>
          )}
        </div>

        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-indigo-900 rounded-2xl p-6 sm:p-8 text-white shadow-md mb-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-2 mb-3">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-semibold border border-emerald-500/30">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  Authenticated Role: {user.role}
                </span>

                {user.responsibilities?.map((r) => (
                  <span
                    key={r.id}
                    className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-indigo-500/30 text-indigo-200 text-xs font-medium border border-indigo-400/30"
                  >
                    <Award className="w-3 h-3" />
                    {r.type.replace('_', ' ')} {r.sectionName ? `(${r.sectionName})` : ''}
                  </span>
                ))}
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                Welcome, {user.fullName}
              </h1>
              <p className="mt-1 text-xs sm:text-sm text-indigo-200">
                Institutional ID: <span className="font-mono text-white font-semibold">{user.registrationNumber || user.uniqueId || user.username}</span> &bull; {user.email}
              </p>
            </div>

            <div className="flex items-center gap-3">
              {(user.role === 'ADMIN' || user.role === 'PRINCIPAL' || user.role === 'HOD' || isUserHod(user)) && (
                <Link
                  to="/users"
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-indigo-500 hover:bg-indigo-600 text-white font-semibold text-xs sm:text-sm transition-colors shadow-sm"
                >
                  <Users className="w-4 h-4" />
                  Manage Users & Roles
                </Link>
              )}
            </div>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center p-16">
            <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
            <span className="ml-3 text-sm font-medium text-slate-600">Loading your academic workspace...</span>
          </div>
        ) : (
          <>
            {/* ============================================================== */}
            {/* 1. STUDENT VIEW                                                */}
            {/* ============================================================== */}
            {user.role === 'STUDENT' && studentProfile && (
              <div className="space-y-6">
                {/* Academic Placement Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
                      <span>Academic Placement</span>
                      <GraduationCap className="w-4 h-4 text-indigo-600" />
                    </div>
                    <div className="text-base font-bold text-slate-900 mt-1">
                      {studentProfile.placement?.programName || studentProfile.programName || 'Degree Program'}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      {studentProfile.placement?.academicYearName || studentProfile.academicYearName || ''} &bull; {studentProfile.placement?.semesterName || studentProfile.semesterName || ''}
                    </div>
                    <div className="mt-2.5 flex flex-wrap gap-1.5 items-center">
                      <span className="px-2 py-0.5 bg-indigo-50 text-indigo-700 font-bold rounded text-xs border border-indigo-100">
                        {studentProfile.placement?.sectionName || studentProfile.sectionName || 'Section'}
                      </span>
                      {studentProfile.admissionCategory && (
                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 font-medium rounded text-[11px] border border-slate-200">
                          Quota: {studentProfile.admissionCategory}
                        </span>
                      )}
                      {studentProfile.tuitionCategory && (
                        <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 font-medium rounded text-[11px] border border-emerald-100">
                          {studentProfile.tuitionCategory}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
                      <span>Designated Class Teacher</span>
                      <Award className="w-4 h-4 text-teal-600" />
                    </div>
                    <div className="text-base font-bold text-slate-900 mt-1">
                      {studentProfile.classTeacherName || 'Not Assigned'}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      Oversees section academic attendance & progress
                    </div>
                    <div className="mt-2 text-xs text-teal-700 font-semibold">
                      Section Mentor
                    </div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
                      <span>Assigned Counsellor</span>
                      <HeartHandshake className="w-4 h-4 text-indigo-600" />
                    </div>
                    <div className="text-base font-bold text-slate-900 mt-1">
                      {studentProfile.counsellorName || 'Not Assigned'}
                    </div>
                    <div className="text-xs text-slate-500 mt-0.5">
                      Personal academic mentorship & support
                    </div>
                    <div className="mt-2 text-xs text-indigo-700 font-semibold">
                      {studentCounselling.length} Session(s) Logged
                    </div>
                  </div>
                </div>

                {/* Today's Timetable Schedule for Student */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 mb-4 gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                        <Calendar className="w-5 h-5" />
                      </div>
                      <div>
                        <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                          Today's Academic Schedule
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-indigo-100 text-indigo-800">
                            {todayWeekday}
                          </span>
                        </h2>
                        <p className="text-xs text-slate-500">
                          {studentTimetable.filter((t) => t.dayOfWeek?.toUpperCase() === todayWeekday.toUpperCase()).length > 0
                            ? `You have ${studentTimetable.filter((t) => t.dayOfWeek?.toUpperCase() === todayWeekday.toUpperCase()).length} period(s) scheduled for today.`
                            : `No lectures scheduled for ${todayWeekday}.`}
                        </p>
                      </div>
                    </div>
                    <Link
                      to="/timetable"
                      className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 inline-flex items-center gap-1"
                    >
                      Full Weekly Timetable &rarr;
                    </Link>
                  </div>

                  {studentTimetable.filter((t) => t.dayOfWeek?.toUpperCase() === todayWeekday.toUpperCase()).length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                      {studentTimetable
                        .filter((t) => t.dayOfWeek?.toUpperCase() === todayWeekday.toUpperCase())
                        .sort((a, b) => a.periodNumber - b.periodNumber)
                        .map((t) => (
                          <div
                            key={t.id}
                            className="bg-slate-50 border border-slate-200/80 rounded-xl p-3.5 hover:shadow-xs transition-shadow"
                          >
                            <div className="flex items-center justify-between text-xs text-slate-500 mb-1.5">
                              <span className="font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100 text-[11px]">
                                Period {t.periodNumber}
                              </span>
                              <span className="inline-flex items-center gap-1 font-mono text-[11px] text-slate-600">
                                <Clock className="w-3 h-3 text-slate-400" />
                                {t.startTime} - {t.endTime}
                              </span>
                            </div>
                            <div className="font-bold text-slate-900 text-sm leading-tight mt-1">
                              {t.subjectName}
                            </div>
                            <div className="text-xs font-mono text-slate-500 mt-0.5">
                              {t.subjectCode} &bull; <span className="font-sans font-medium text-slate-600">{t.sessionType || 'LECTURE'}</span>
                            </div>
                            <div className="mt-2.5 pt-2 border-t border-slate-200/60 flex items-center justify-between text-xs text-slate-600">
                              <span className="truncate max-w-[140px]" title={t.facultyName}>
                                {t.facultyName || 'Faculty'}
                              </span>
                              {t.roomNumber && (
                                <span className="font-mono text-[11px] text-slate-500 bg-white px-1.5 py-0.5 rounded border border-slate-200">
                                  {t.roomNumber}
                                </span>
                              )}
                            </div>
                          </div>
                        ))}
                    </div>
                  ) : (
                    <div className="text-center py-6 bg-slate-50/50 rounded-xl border border-dashed border-slate-200">
                      <Clock className="w-6 h-6 text-slate-400 mx-auto mb-1.5" />
                      <p className="text-xs text-slate-600 font-medium">No classes scheduled for {todayWeekday}.</p>
                      <p className="text-[11px] text-slate-400 mt-0.5">Check your complete weekly timetable for upcoming classes.</p>
                      <Link
                        to="/timetable"
                        className="mt-3 inline-inline-flex items-center text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                      >
                        View Full Timetable &rarr;
                      </Link>
                    </div>
                  )}
                </div>

                {/* Educational History Section */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                    <div className="flex items-center gap-2">
                      <FileText className="w-5 h-5 text-indigo-600" />
                      <h2 className="text-base font-bold text-slate-900">
                        Educational History & Prior Qualifications
                      </h2>
                    </div>
                    <span className="text-xs text-slate-500">
                      Entry Type: <strong className="text-slate-800">{studentProfile.entryType}</strong>
                    </span>
                  </div>

                  {studentProfile.educationalHistory && studentProfile.educationalHistory.length > 0 ? (
                    <div className="overflow-x-auto">
                      <table className="w-full text-xs text-left">
                        <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                          <tr>
                            <th className="py-2.5 px-3">Qualification</th>
                            <th className="py-2.5 px-3">Board / University</th>
                            <th className="py-2.5 px-3">Institution</th>
                            <th className="py-2.5 px-3">Hall Ticket No.</th>
                            <th className="py-2.5 px-3 text-center">Year</th>
                            <th className="py-2.5 px-3 text-right">Percentage / CGPA</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {studentProfile.educationalHistory.map((edu) => (
                            <tr key={edu.id} className="hover:bg-slate-50">
                              <td className="py-3 px-3 font-bold text-indigo-900">
                                {edu.qualificationType}
                              </td>
                              <td className="py-3 px-3 text-slate-700">{edu.boardOrUniversity}</td>
                              <td className="py-3 px-3 text-slate-700">{edu.institutionName}</td>
                              <td className="py-3 px-3 font-mono text-slate-600">{edu.hallTicketNumber || 'N/A'}</td>
                              <td className="py-3 px-3 text-center text-slate-700">{edu.completionYear}</td>
                              <td className="py-3 px-3 text-right font-bold text-emerald-700">
                                {edu.percentage ? `${edu.percentage}%` : edu.gradeOrCgpa || 'N/A'}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 py-3">No prior educational records submitted.</p>
                  )}
                </div>

                {/* Counselling Logs Section */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                    <div className="flex items-center gap-2">
                      <HeartHandshake className="w-5 h-5 text-indigo-600" />
                      <h2 className="text-base font-bold text-slate-900">
                        Counselling Sessions & Action Plans
                      </h2>
                    </div>
                    <span className="text-xs text-slate-500">
                      Counsellor: <strong className="text-slate-800">{studentProfile.counsellorName || 'Assigned Mentor'}</strong>
                    </span>
                  </div>

                  {studentCounselling.length > 0 ? (
                    <div className="space-y-4">
                      {studentCounselling.map((rec) => (
                        <div key={rec.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50">
                          <div className="flex items-center justify-between mb-2">
                            <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">
                              {rec.recordType}
                            </span>
                            <span className="text-xs text-slate-500 flex items-center gap-1">
                              <Calendar className="w-3.5 h-3.5" /> Meeting Date: {rec.meetingDate}
                            </span>
                          </div>
                          <p className="text-xs text-slate-800 font-medium mb-2">
                            <strong>Remarks:</strong> {rec.remarks}
                          </p>
                          {rec.actionPlan && (
                            <p className="text-xs text-slate-600">
                              <strong>Action Plan:</strong> {rec.actionPlan}
                            </p>
                          )}
                          <div className="mt-3 pt-2 border-t border-slate-200 text-[11px] text-slate-500 flex justify-between">
                            <span>Status: <strong className="uppercase">{rec.status}</strong></span>
                            {rec.followUpDate && <span>Follow-up: {rec.followUpDate}</span>}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500 py-3">No counselling meetings logged yet.</p>
                  )}
                </div>
              </div>
            )}

            {/* ============================================================== */}
            {/* 2. FACULTY VIEW                                                */}
            {/* ============================================================== */}
            {user.role === 'FACULTY' && (
              <div className="space-y-6">
                {/* Class Teacher Banner if assigned */}
                {sectionOverview && (
                  <div className="bg-gradient-to-r from-teal-900 to-teal-800 rounded-xl p-5 text-white shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-teal-700/50 flex items-center justify-center font-bold">
                        <Award className="w-5 h-5 text-teal-200" />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-teal-200 uppercase tracking-wider">
                          Assigned Class Teacher Responsibility
                        </div>
                        <h2 className="text-base font-bold text-white mt-0.5">
                          {sectionOverview.sectionName} &bull; {sectionOverview.programName}
                        </h2>
                        <div className="text-xs text-teal-100 mt-0.5">
                          {sectionOverview.academicYearName} &bull; {sectionOverview.semesterName}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <div className="text-lg font-extrabold">{sectionOverview.totalStudents} Students</div>
                        <div className="text-[11px] text-teal-200">
                          {sectionOverview.regularStudents} Regular &bull; {sectionOverview.lateralStudents} Lateral
                        </div>
                      </div>
                      <Link
                        to="/students"
                        className="px-3.5 py-2 rounded-lg bg-white text-teal-900 font-bold text-xs hover:bg-teal-50 transition-colors shadow-2xs"
                      >
                        View Class Roster &rarr;
                      </Link>
                    </div>
                  </div>
                )}

                {/* Prominently Featured: Today's Scheduled Classes */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 mb-4 gap-2">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                        <Calendar className="w-5 h-5" />
                      </div>
                      <div>
                        <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                          Today's Scheduled Classes
                          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-indigo-100 text-indigo-800">
                            {todayWeekday}
                          </span>
                        </h2>
                        <p className="text-xs text-slate-500">
                          {todayClasses.length > 0
                            ? `You have ${todayClasses.length} class session${todayClasses.length > 1 ? 's' : ''} scheduled for today.`
                            : 'Classes scheduled chronologically for today.'}
                        </p>
                      </div>
                    </div>
                    <Link
                      to="/timetable"
                      className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 inline-flex items-center gap-1"
                    >
                      Weekly Timetable &rarr;
                    </Link>
                  </div>

                  {todayClasses.length === 0 ? (
                    <div className="p-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">
                      <Clock className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                      <p className="text-sm font-semibold text-slate-700">No classes scheduled today</p>
                      <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                        You have no teaching periods assigned for {todayWeekday}. Check your full weekly timetable for upcoming classes.
                      </p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {todayClasses.map((cls) => (
                        <div
                          key={cls.id}
                          className="p-4 rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50/80 hover:border-indigo-300 transition-all shadow-2xs"
                        >
                          <div className="flex items-center justify-between mb-2">
                            <span className="px-2 py-0.5 rounded font-mono font-bold text-xs bg-indigo-50 text-indigo-700 border border-indigo-100">
                              {cls.subjectCode}
                            </span>
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              {cls.roomNumber || 'LH-301'}
                            </span>
                          </div>

                          <h3 className="text-sm font-bold text-slate-900 line-clamp-1 mb-1" title={cls.subjectName}>
                            {cls.subjectName}
                          </h3>

                          <div className="text-xs text-slate-500 mb-3">
                            <span className="font-semibold text-indigo-900">{cls.sectionName}</span>
                            {cls.programName && <span> &bull; {cls.programName}</span>}
                          </div>

                          <div className="pt-3 border-t border-slate-200 flex items-center justify-between text-xs">
                            <span className="font-bold text-slate-700">
                              Period {cls.periodNumber}
                            </span>
                            <span className="font-mono font-bold text-indigo-700 flex items-center gap-1">
                              <Clock className="w-3.5 h-3.5 text-indigo-500" />
                              {cls.startTime?.substring(0, 5)} - {cls.endTime?.substring(0, 5)}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Counsellor Mentorship Section if assigned */}
                {counsellingStudents.length > 0 && (
                  <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                    <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                      <div className="flex items-center gap-2">
                        <HeartHandshake className="w-5 h-5 text-indigo-600" />
                        <div>
                          <h2 className="text-base font-bold text-slate-900">
                            My Assigned Mentoring & Counselling Students
                          </h2>
                          <p className="text-xs text-slate-500">
                            You are designated academic counsellor for {counsellingStudents.length} students &bull; {counsellingRecords.length} Session(s) Recorded
                          </p>
                        </div>
                      </div>

                      <button
                        onClick={() => setShowRecordModal(true)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-colors shadow-2xs cursor-pointer"
                      >
                        <PlusCircle className="w-3.5 h-3.5" />
                        Log Session
                      </button>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-xs text-left">
                        <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                          <tr>
                            <th className="py-2.5 px-3">Roll No</th>
                            <th className="py-2.5 px-3">Student Name</th>
                            <th className="py-2.5 px-3">Section</th>
                            <th className="py-2.5 px-3">Entry Type</th>
                            <th className="py-2.5 px-3 text-center">Sessions</th>
                            <th className="py-2.5 px-3">Last Meeting</th>
                            <th className="py-2.5 px-3">Status</th>
                            <th className="py-2.5 px-3 text-right">Profile</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {counsellingStudents.map((s) => (
                            <tr
                              key={s.studentId}
                              onClick={() => navigate(`/students/${s.studentId}`)}
                              className="hover:bg-indigo-50/70 cursor-pointer transition-colors group"
                              title="Click to view detailed student profile"
                            >
                              <td className="py-2.5 px-3 font-mono font-bold text-indigo-900 group-hover:text-indigo-700">
                                {s.registrationNumber}
                              </td>
                              <td className="py-2.5 px-3 font-semibold text-slate-800 group-hover:text-indigo-900">
                                {s.fullName}
                              </td>
                              <td className="py-2.5 px-3 text-slate-600">{s.sectionName}</td>
                              <td className="py-2.5 px-3 text-slate-600">{s.entryType}</td>
                              <td className="py-2.5 px-3 text-center font-bold text-indigo-700">
                                {s.totalSessions}
                              </td>
                              <td className="py-2.5 px-3 text-slate-600">
                                {s.lastMeetingDate || 'None'}
                              </td>
                              <td className="py-2.5 px-3">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  s.lastStatus === 'OPEN' ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'
                                }`}>
                                  {s.lastStatus}
                                </span>
                              </td>
                              <td className="py-2.5 px-3 text-right">
                                <span className="inline-flex items-center text-slate-400 group-hover:text-indigo-600 font-semibold text-[11px] gap-0.5">
                                  View <ChevronRight className="w-3.5 h-3.5" />
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* Teaching Assignments Grid */}
                <div>
                  <h2 className="text-base font-bold text-slate-900 mb-1 flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-indigo-600" />
                    My Assigned Teaching Subjects & Classes
                  </h2>
                  <p className="text-xs text-slate-500 mb-3">
                    Teaching across {facultyClasses.length} class section(s)
                  </p>

                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {facultySubjects.map((sub) => (
                      <div key={sub.subjectId + sub.sectionId} className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-300 transition-colors">
                        <div className="flex items-center justify-between mb-2">
                          <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                            {sub.code}
                          </span>
                          <span className="text-[11px] font-semibold text-slate-500">
                            {sub.credits} Credits &bull; {sub.type}
                          </span>
                        </div>
                        <h3 className="text-sm font-bold text-slate-900 leading-snug">
                          {sub.name}
                        </h3>
                        <p className="text-xs text-slate-500 mt-1">
                          {sub.programName} &bull; {sub.semesterName}
                        </p>

                        <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                          <span className="text-xs font-bold text-slate-700 bg-slate-100 px-2.5 py-1 rounded">
                            {sub.sectionName}
                          </span>
                          <span className="text-xs text-indigo-600 font-semibold">
                            Taught by you
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Faculty Timetable Snapshot */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                    <div className="flex items-center gap-2">
                      <Clock className="w-5 h-5 text-indigo-600" />
                      <h2 className="text-base font-bold text-slate-900">
                        My Teaching Timetable Schedule
                      </h2>
                    </div>
                    <Link to="/timetable" className="text-xs font-bold text-indigo-600 hover:text-indigo-800">
                      View Full Schedule &rarr;
                    </Link>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                        <tr>
                          <th className="py-2.5 px-3">Day</th>
                          <th className="py-2.5 px-3 text-center">Period</th>
                          <th className="py-2.5 px-3">Time</th>
                          <th className="py-2.5 px-3">Subject</th>
                          <th className="py-2.5 px-3">Section</th>
                          <th className="py-2.5 px-3">Room</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {facultyTimetable.slice(0, 8).map((slot) => (
                          <tr key={slot.id} className="hover:bg-slate-50">
                            <td className="py-2.5 px-3 font-semibold text-slate-800">{slot.dayOfWeek}</td>
                            <td className="py-2.5 px-3 text-center font-bold text-indigo-700">Period {slot.periodNumber}</td>
                            <td className="py-2.5 px-3 text-slate-600">{slot.startTime} - {slot.endTime}</td>
                            <td className="py-2.5 px-3 font-medium text-slate-900">{slot.subjectCode} - {slot.subjectName}</td>
                            <td className="py-2.5 px-3 font-bold text-slate-700">{slot.sectionName}</td>
                            <td className="py-2.5 px-3 font-mono text-emerald-700 font-bold">{slot.roomNumber || 'LH-301'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* ============================================================== */}
            {/* 3. HOD VIEW                                                    */}
            {/* ============================================================== */}
            {(user.role === 'HOD' || isUserHod(user)) && hodHierarchy && (
              <div className="space-y-6">
                {/* Department KPI Bar */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                      Department
                    </div>
                    <div className="text-2xl font-extrabold text-slate-900">{hodHierarchy.departmentCode}</div>
                    <div className="text-xs text-slate-500 mt-1">{hodHierarchy.departmentName}</div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                      Head of Department
                    </div>
                    <div className="text-lg font-bold text-slate-900">{hodHierarchy.hodName}</div>
                    <div className="text-xs text-slate-500 mt-1">Full Scope CSE</div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                      Faculty Count
                    </div>
                    <div className="text-2xl font-extrabold text-indigo-700">{hodHierarchy.totalFaculty ?? hodFaculty.length}</div>
                    <div className="text-xs text-slate-500 mt-1">Active Professors</div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                      Department Students
                    </div>
                    <div className="text-2xl font-extrabold text-emerald-700">
                      {hodHierarchy.totalStudents || hodHierarchy.academicYears.flatMap((ay) => ay.programs.flatMap((p) => p.semesters.flatMap((s) => s.sections))).reduce((acc, sec) => acc + (sec.studentCount || 0), 0) || 736}
                    </div>
                    <div className="text-xs text-slate-500 mt-1">Enrolled across sections</div>
                  </div>

                  <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                    <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                      Curriculum Subjects
                    </div>
                    <div className="text-2xl font-extrabold text-purple-700">{hodSubjects.length}</div>
                    <div className="text-xs text-slate-500 mt-1">Active Department Subjects</div>
                  </div>
                </div>

                {/* Hierarchical Academic Structure */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-5">
                    <div className="flex items-center gap-2">
                      <Layers className="w-5 h-5 text-indigo-600" />
                      <div>
                        <h2 className="text-base font-bold text-slate-900">
                          Academic Hierarchy: Academic Years &rarr; Programs &rarr; Semesters &rarr; Sections
                        </h2>
                        <p className="text-xs text-slate-500">
                          Structured departmental hierarchy (No flat section directory)
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-6">
                    {hodHierarchy.academicYears.map((ay) => (
                      <div key={ay.academicYearId} className="border border-slate-200 rounded-xl p-4 bg-slate-50/50">
                        <div className="flex items-center justify-between mb-3">
                          <span className="font-bold text-sm text-slate-900 flex items-center gap-2">
                            <Calendar className="w-4 h-4 text-indigo-600" />
                            Academic Year: {ay.yearName}
                            {ay.isCurrent && (
                              <span className="px-2 py-0.5 text-[10px] bg-emerald-100 text-emerald-800 font-bold rounded">
                                CURRENT
                              </span>
                            )}
                          </span>
                        </div>

                        {ay.programs.map((prog) => {
                          const pName = prog.programName || prog.name || 'Program';
                          const pCode = prog.programCode || prog.code || '';
                          return (
                            <div key={prog.programId || prog.id} className="bg-white rounded-lg p-4 border border-slate-200 mb-3 last:mb-0">
                              <div className="font-bold text-xs text-indigo-900 mb-2 flex items-center justify-between">
                                <span>{prog.degree} in {pName} {pCode ? `(${pCode})` : ''}</span>
                              </div>

                              <div className="space-y-3">
                                {prog.semesters.map((sem) => (
                                  <div key={sem.semesterId || sem.id} className="pl-4 border-l-2 border-indigo-200 py-1">
                                    <div className="text-xs font-semibold text-slate-700 mb-2">
                                      {sem.termName} (Semester {sem.semesterNumber})
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                                      {sem.sections.map((sec) => (
                                        <Link
                                          key={sec.sectionId || sec.id}
                                          to={`/sections/${sec.sectionId || sec.id}`}
                                          className="p-3 bg-slate-50 hover:bg-indigo-50/70 rounded-lg border border-slate-200 hover:border-indigo-300 transition-all cursor-pointer group block"
                                          title="Click to view full Section Overview (Timetable, Subjects, Student Roster)"
                                        >
                                          <div className="flex items-center justify-between">
                                            <span className="font-bold text-xs text-slate-900 group-hover:text-indigo-900 flex items-center gap-1">
                                              {sec.sectionName}
                                              <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:text-indigo-600 transition-transform group-hover:translate-x-0.5" />
                                            </span>
                                            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                                              {sec.studentCount} Students
                                            </span>
                                          </div>
                                          <div className="text-[11px] text-slate-500 mt-2">
                                            Class Teacher: <strong className="text-slate-800">{sec.classTeacherName || 'Unassigned'}</strong>
                                          </div>
                                        </Link>
                                      ))}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Faculty Directory in Department */}
                <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                    <div className="flex items-center gap-2">
                      <Users className="w-5 h-5 text-indigo-600" />
                      <h2 className="text-base font-bold text-slate-900">
                        Department Faculty Profiles & Specializations
                      </h2>
                    </div>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                        <tr>
                          <th className="py-2.5 px-3">Employee No.</th>
                          <th className="py-2.5 px-3">Designation</th>
                          <th className="py-2.5 px-3">Qualification</th>
                          <th className="py-2.5 px-3">Specialization</th>
                          <th className="py-2.5 px-3">Experience</th>
                          <th className="py-2.5 px-3">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {hodFaculty.map((f) => (
                          <tr key={f.id} className="hover:bg-slate-50">
                            <td className="py-2.5 px-3 font-mono font-bold text-slate-900">{f.employeeNumber}</td>
                            <td className="py-2.5 px-3 font-semibold text-indigo-900">{f.designation}</td>
                            <td className="py-2.5 px-3 text-slate-700">{f.qualification || 'M.Tech'}</td>
                            <td className="py-2.5 px-3 text-slate-700">{f.specialization || 'CSE'}</td>
                            <td className="py-2.5 px-3 text-slate-600">{f.professionalExperience || '10+ Years'}</td>
                            <td className="py-2.5 px-3">
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                                {f.activeStatus}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* ============================================================== */}
            {/* 4. PRINCIPAL VIEW                                              */}
            {/* ============================================================== */}
            {user.role === 'PRINCIPAL' && collegeOverview && (
              <div className="space-y-6">
                {selectedDeptHierarchy ? (
                  /* Drilled-down Department View */
                  <div className="space-y-6">
                    <div className="flex items-center justify-between">
                      <button
                        onClick={() => setSearchParams({})}
                        className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 hover:text-indigo-600 font-semibold text-xs transition-colors shadow-2xs cursor-pointer"
                      >
                        <ArrowLeft className="w-4 h-4" /> Back to College Overview
                      </button>
                      <span className="text-xs text-slate-500 font-medium">
                        Inspecting Department: <strong>{selectedDeptHierarchy.departmentName}</strong>
                      </span>
                    </div>

                    {/* Department Header Card */}
                    <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                        <div>
                          <div className="flex items-center gap-2">
                            <h2 className="text-xl font-bold text-slate-900">
                              {selectedDeptHierarchy.departmentName}
                            </h2>
                            <span className="px-2 py-0.5 rounded bg-indigo-100 text-indigo-800 font-mono font-bold text-xs">
                              {selectedDeptHierarchy.departmentCode}
                            </span>
                          </div>
                          <p className="text-xs text-slate-500 mt-1">
                            Head of Department: <strong className="text-slate-800">{selectedDeptHierarchy.hodName}</strong>
                          </p>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="bg-indigo-50 border border-indigo-200 text-indigo-800 px-3 py-1.5 rounded-lg text-xs font-bold">
                            {selectedDeptHierarchy.totalFaculty} Active Faculty
                          </span>
                          <span className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-3 py-1.5 rounded-lg text-xs font-bold">
                            {selectedDeptHierarchy.totalStudents} Enrolled Students
                          </span>
                        </div>
                      </div>

                      {/* Department Structure Tree */}
                      <div className="mt-6 space-y-6">
                        <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                          <Layers className="w-4 h-4 text-indigo-600" />
                          Department Academic Structure: Programs &rarr; Semesters &rarr; Sections
                        </h3>

                        {selectedDeptHierarchy.academicYears.map((ay) => (
                          <div key={ay.academicYearId || ay.id} className="border border-slate-200 rounded-xl p-4 bg-slate-50/50">
                            <div className="flex items-center justify-between mb-3">
                              <span className="font-bold text-sm text-slate-900 flex items-center gap-2">
                                <Calendar className="w-4 h-4 text-indigo-600" />
                                Academic Year: {ay.yearName}
                                {(ay.isCurrent || ay.current) && (
                                  <span className="px-2 py-0.5 text-[10px] bg-emerald-100 text-emerald-800 font-bold rounded">
                                    CURRENT
                                  </span>
                                )}
                              </span>
                            </div>

                            {ay.programs.map((prog) => {
                              const pName = prog.programName || prog.name || 'Program';
                              const pCode = prog.programCode || prog.code || '';
                              return (
                                <div key={prog.programId || prog.id} className="bg-white rounded-lg p-4 border border-slate-200 mb-3 last:mb-0">
                                  <div className="font-bold text-xs text-indigo-900 mb-2">
                                    {prog.degree} in {pName} {pCode ? `(${pCode})` : ''}
                                  </div>

                                  <div className="space-y-3">
                                    {prog.semesters.map((sem) => (
                                      <div key={sem.semesterId || sem.id} className="pl-4 border-l-2 border-indigo-200 py-1">
                                        <div className="text-xs font-semibold text-slate-700 mb-2">
                                          {sem.termName} (Semester {sem.semesterNumber})
                                        </div>

                                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                                          {sem.sections.map((sec) => (
                                            <Link
                                              key={sec.sectionId || sec.id}
                                              to={`/sections/${sec.sectionId || sec.id}`}
                                              className="p-3 bg-slate-50 hover:bg-indigo-50/70 rounded-lg border border-slate-200 hover:border-indigo-300 transition-all cursor-pointer group block"
                                              title="Click to view full Section Overview (Timetable, Subjects, Student Roster)"
                                            >
                                              <div className="flex items-center justify-between">
                                                <span className="font-bold text-xs text-slate-900 group-hover:text-indigo-900 flex items-center gap-1">
                                                  {sec.sectionName}
                                                  <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:text-indigo-600 transition-transform group-hover:translate-x-0.5" />
                                                </span>
                                                <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                                                  {sec.studentCount} Students
                                                </span>
                                              </div>
                                              <div className="text-[11px] text-slate-500 mt-2">
                                                Class Teacher: <strong className="text-slate-800">{sec.classTeacherName || 'Unassigned'}</strong>
                                              </div>
                                            </Link>
                                          ))}
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                ) : loadingDept ? (
                  <div className="bg-white rounded-xl p-12 text-center border border-slate-200">
                    <Loader2 className="w-8 h-8 text-indigo-600 animate-spin mx-auto mb-2" />
                    <p className="text-xs text-slate-500 font-semibold">Loading Department Academic Structure...</p>
                  </div>
                ) : (
                  /* College-level Overview */
                  <>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                        <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                          Institution
                        </div>
                        <div className="text-2xl font-extrabold text-slate-900">{collegeOverview.code}</div>
                        <div className="text-xs text-slate-500 mt-1">{collegeOverview.institutionName}</div>
                      </div>

                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                        <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                          Departments
                        </div>
                        <div className="text-2xl font-extrabold text-indigo-700">{collegeOverview.totalDepartments}</div>
                        <div className="text-xs text-slate-500 mt-1">{collegeOverview.totalPrograms} Programs Total</div>
                      </div>

                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                        <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                          Total Faculty
                        </div>
                        <div className="text-2xl font-extrabold text-indigo-700">{collegeOverview.totalFaculty}</div>
                        <div className="text-xs text-slate-500 mt-1">Across all departments</div>
                      </div>

                      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
                        <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1">
                          Total Students
                        </div>
                        <div className="text-2xl font-extrabold text-emerald-700">{collegeOverview.totalStudents}</div>
                        <div className="text-xs text-slate-500 mt-1">College-wide Enrollment</div>
                      </div>
                    </div>

                    <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                      <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
                        <Building className="w-5 h-5 text-indigo-600" />
                        Departments & Programs Overview (Click department to drill down)
                      </h2>

                      <div className="space-y-4">
                        {collegeOverview.departments.map((dept) => (
                          <div key={dept.departmentId || dept.id} className="border border-slate-200 rounded-xl p-5 bg-slate-50 hover:border-indigo-300 transition-colors">
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-200 gap-2 mb-4">
                              <div>
                                <span className="font-bold text-sm text-slate-900">
                                  {dept.name} ({dept.code})
                                </span>
                                <div className="text-xs text-slate-500">
                                  HOD: <strong className="text-slate-800">{dept.hodName || 'Not Assigned'}</strong>
                                </div>
                              </div>
                              <div className="flex items-center gap-3 text-xs">
                                <span className="bg-indigo-100 text-indigo-800 px-2.5 py-1 rounded font-bold">
                                  {dept.facultyCount} Faculty
                                </span>
                                <span className="bg-emerald-100 text-emerald-800 px-2.5 py-1 rounded font-bold">
                                  {dept.studentCount} Students
                                </span>
                                <button
                                  onClick={() => setSearchParams({ dept: dept.departmentId || dept.id || '' })}
                                  className="px-3 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs transition-colors shadow-2xs cursor-pointer flex items-center gap-1"
                                >
                                  Drill Down &rarr;
                                </button>
                              </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                              {dept.programs.map((p) => (
                                <div key={p.programId || p.code} className="p-3 bg-white rounded-lg border border-slate-200 shadow-2xs">
                                  <div className="font-bold text-xs text-indigo-900">{p.name}</div>
                                  <div className="text-[11px] text-slate-500 mt-1">{p.degree} &bull; Code: {p.code}</div>
                                  <div className="text-xs font-bold text-emerald-700 mt-2">{p.studentCount} Students Enrolled</div>
                                </div>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </>
                )}
              </div>
            )}

            {/* ============================================================== */}
            {/* 5. ADMIN VIEW                                                  */}
            {/* ============================================================== */}
            {user.role === 'ADMIN' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <Link to="/users" className="bg-white p-6 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-400 transition-all group">
                    <Users className="w-8 h-8 text-indigo-600 mb-3 group-hover:scale-105 transition-transform" />
                    <h3 className="text-base font-bold text-slate-900">User Management</h3>
                    <p className="text-xs text-slate-500 mt-1">Manage accounts, assign roles, manage teaching responsibilities.</p>
                  </Link>
                  <Link to="/students" className="bg-white p-6 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-400 transition-all group">
                    <BookOpen className="w-8 h-8 text-emerald-600 mb-3 group-hover:scale-105 transition-transform" />
                    <h3 className="text-base font-bold text-slate-900">Student Directory</h3>
                    <p className="text-xs text-slate-500 mt-1">View enrolled students across programs and sections.</p>
                  </Link>
                  <Link to="/timetable" className="bg-white p-6 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-400 transition-all group">
                    <Calendar className="w-8 h-8 text-amber-600 mb-3 group-hover:scale-105 transition-transform" />
                    <h3 className="text-base font-bold text-slate-900">Academic Timetable</h3>
                    <p className="text-xs text-slate-500 mt-1">Class schedules, periods, subject allocations.</p>
                  </Link>
                </div>
              </div>
            )}
          </>
        )}
      </main>

      {/* Log Counselling Session Modal */}
      {showRecordModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-2xs p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200">
            <h2 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
              <HeartHandshake className="w-5 h-5 text-indigo-600" />
              Log Student Counselling Session
            </h2>

            <form onSubmit={handleCreateCounsellingRecord} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Select Mentee
                </label>
                <select
                  value={selectedMentee}
                  onChange={(e) => setSelectedMentee(e.target.value)}
                  className="w-full text-xs p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                  required
                >
                  {counsellingStudents.map((s) => (
                    <option key={s.studentId} value={s.studentId}>
                      {s.registrationNumber} - {s.fullName} ({s.sectionName})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Meeting Date
                  </label>
                  <input
                    type="date"
                    value={meetingDate}
                    onChange={(e) => setMeetingDate(e.target.value)}
                    className="w-full text-xs p-2 border border-slate-300 rounded-lg"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Record Type
                  </label>
                  <select
                    value={recordType}
                    onChange={(e) => setRecordType(e.target.value)}
                    className="w-full text-xs p-2 border border-slate-300 rounded-lg"
                  >
                    <option value="GENERAL">General Mentoring</option>
                    <option value="ACADEMIC">Academic Guidance</option>
                    <option value="ATTENDANCE_WARNING">Attendance Advisory</option>
                    <option value="CAREER">Career & Placement</option>
                    <option value="FOLLOW_UP">Follow-up Session</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Session Discussion / Remarks
                </label>
                <textarea
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  placeholder="Record summary of discussion with student..."
                  rows={3}
                  className="w-full text-xs p-2.5 border border-slate-300 rounded-lg"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Action Plan & Goals
                </label>
                <textarea
                  value={actionPlan}
                  onChange={(e) => setActionPlan(e.target.value)}
                  placeholder="Steps agreed upon for improvement..."
                  rows={2}
                  className="w-full text-xs p-2.5 border border-slate-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Next Follow-up Date (Optional)
                </label>
                <input
                  type="date"
                  value={followUpDate}
                  onChange={(e) => setFollowUpDate(e.target.value)}
                  className="w-full text-xs p-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowRecordModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingRecord}
                  className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
                >
                  {submittingRecord ? 'Saving...' : 'Save Record'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
