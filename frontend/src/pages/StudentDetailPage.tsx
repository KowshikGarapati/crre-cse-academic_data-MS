import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { studentApi } from '../api/student.api';
import { academicApi } from '../api/academic.api';
import type {
  AcademicMetadata,
  CertificateStatus,
  StudentCertificateDto,
  StudentDetailDto,
  UpsertCertificateRequest,
} from '../types/auth.types';
import { StudentEditModal } from '../components/StudentEditModal';
import {
  ArrowLeft,
  GraduationCap,
  Building,
  Calendar,
  Clock,
  BookOpen,
  HeartHandshake,
  Award,
  Loader2,
  AlertTriangle,
  FileText,
  CheckCircle2,
  Lock,
  Edit,
  ShieldCheck,
  Plus,
  Trash2,
  X,
} from 'lucide-react';

export const StudentDetailPage: React.FC = () => {
  const { studentId } = useParams<{ studentId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [student, setStudent] = useState<StudentDetailDto | null>(null);
  const [metadata, setMetadata] = useState<AcademicMetadata | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedDay, setSelectedDay] = useState<string>('ALL');
  const [showEditModal, setShowEditModal] = useState(false);

  // Certificates State (Phase 4)
  const [certificates, setCertificates] = useState<StudentCertificateDto[]>([]);
  const [loadingCerts, setLoadingCerts] = useState(false);
  const [showCertModal, setShowCertModal] = useState(false);
  const [editingCertId, setEditingCertId] = useState<string | null>(null);
  const [certForm, setCertForm] = useState<UpsertCertificateRequest>({
    certificateType: 'SSC_MARKS_MEMO',
    certificateNumber: '',
    issuingAuthority: 'Board of Secondary Education, AP (BSEAP)',
    issueYear: 2024,
    status: 'SUBMITTED',
    remarks: '',
  });
  const [savingCert, setSavingCert] = useState(false);
  const [certModalError, setCertModalError] = useState<string | null>(null);

  useEffect(() => {
    if (user?.role === 'STUDENT') {
      navigate('/', { replace: true });
      return;
    }
    academicApi.getMetadata().then((data) => setMetadata(data)).catch(() => {});
  }, [user, navigate]);

  useEffect(() => {
    if (!studentId) return;

    let isMounted = true;

    studentApi
      .getStudentDetails(studentId)
      .then((data) => {
        if (isMounted) {
          setStudent(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('Failed to load student details', err);
          setError(
            err instanceof Error
              ? err.message
              : 'Access denied: You are not authorized to view this student or record does not exist.'
          );
        }
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [studentId]);

  useEffect(() => {
    if (!student?.id) return;
    setLoadingCerts(true);
    studentApi
      .getStudentCertificates(student.id)
      .then((data) => setCertificates(data))
      .catch((err) => console.error('Failed to load certificates', err))
      .finally(() => setLoadingCerts(false));
  }, [student?.id]);

  const handleOpenAddCert = () => {
    setEditingCertId(null);
    setCertForm({
      certificateType: 'SSC_MARKS_MEMO',
      certificateNumber: '',
      issuingAuthority: 'Board of Secondary Education, AP (BSEAP)',
      issueYear: 2024,
      status: 'SUBMITTED',
      remarks: '',
    });
    setCertModalError(null);
    setShowCertModal(true);
  };

  const handleOpenEditCert = (c: StudentCertificateDto) => {
    setEditingCertId(c.id);
    setCertForm({
      certificateType: c.certificateType,
      certificateNumber: c.certificateNumber || '',
      issuingAuthority: c.issuingAuthority || '',
      issueYear: c.issueYear || 2024,
      status: c.status,
      remarks: c.remarks || '',
    });
    setCertModalError(null);
    setShowCertModal(true);
  };

  const handleSaveCert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!student?.id) return;
    setSavingCert(true);
    setCertModalError(null);
    try {
      if (editingCertId) {
        const updated = await studentApi.updateStudentCertificate(student.id, editingCertId, certForm);
        setCertificates((prev) => prev.map((c) => (c.id === editingCertId ? updated : c)));
      } else {
        const created = await studentApi.addStudentCertificate(student.id, certForm);
        setCertificates((prev) => [created, ...prev]);
      }
      setShowCertModal(false);
    } catch (err: unknown) {
      setCertModalError(err instanceof Error ? err.message : 'Failed to save certificate record');
    } finally {
      setSavingCert(false);
    }
  };

  const handleDeleteCert = async (certId: string) => {
    if (!student?.id || !window.confirm('Are you sure you want to remove this certificate record?')) return;
    try {
      await studentApi.deleteStudentCertificate(student.id, certId);
      setCertificates((prev) => prev.filter((c) => c.id !== certId));
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to delete certificate');
    }
  };

  const daysOfWeek = ['ALL', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

  const timetable = student?.timetable;
  const filteredTimetable = React.useMemo(() => {
    if (!timetable) return [];
    if (selectedDay === 'ALL') return timetable;
    return timetable.filter((t) => t.dayOfWeek.toUpperCase() === selectedDay);
  }, [timetable, selectedDay]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12 flex flex-col items-center justify-center">
          <Loader2 className="w-10 h-10 animate-spin text-indigo-600 mb-3" />
          <h2 className="text-base font-semibold text-slate-800">Loading Student Academic Profile...</h2>
          <p className="text-xs text-slate-500 mt-1">Verifying institutional authorization and compiling student records</p>
        </main>
      </div>
    );
  }

  if (error || !student) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-indigo-600 mb-6 cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" /> Back to Previous View
          </button>

          <div className="bg-white rounded-xl border border-red-200 p-8 shadow-sm text-center">
            <div className="w-14 h-14 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto mb-4">
              <AlertTriangle className="w-7 h-7" />
            </div>
            <h2 className="text-lg font-bold text-slate-900 mb-1">Access Restricted / Student Unavailable</h2>
            <p className="text-sm text-slate-600 max-w-lg mx-auto mb-6">
              {error || 'Unable to retrieve records for this student. You may lack legitimate academic scope, or the identifier is invalid.'}
            </p>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={() => navigate('/students')}
                className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-700 transition shadow-2xs cursor-pointer"
              >
                View Scoped Students Roster
              </button>
              <button
                onClick={() => navigate('/')}
                className="px-4 py-2 rounded-lg bg-slate-100 text-slate-700 text-xs font-semibold hover:bg-slate-200 transition cursor-pointer"
              >
                Return to Dashboard
              </button>
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Navigation & Breadcrumbs */}
        <div className="flex items-center justify-between gap-4 mb-6">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 text-xs font-semibold shadow-2xs transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4 text-slate-500" /> Back to Student List
          </button>

          <div className="text-xs text-slate-500 hidden sm:flex items-center gap-2">
            <Link to="/" className="hover:text-indigo-600">Home</Link>
            <span>/</span>
            <Link to="/students" className="hover:text-indigo-600">Students</Link>
            <span>/</span>
            <span className="font-semibold text-slate-800 font-mono">{student.registrationNumber}</span>
          </div>
        </div>

        {/* Student Profile Header Banner */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-start sm:items-center gap-4">
              <div className="w-14 h-14 rounded-xl bg-indigo-100 text-indigo-700 font-extrabold text-xl flex items-center justify-center shrink-0 border border-indigo-200 shadow-2xs">
                {student.fullName ? student.fullName.charAt(0).toUpperCase() : 'S'}
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="text-xl font-bold text-slate-900">{student.fullName}</h1>
                  <span className="font-mono font-bold text-xs bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded border border-indigo-200">
                    {student.registrationNumber}
                  </span>
                  {student.entryType === 'LATERAL' ? (
                    <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                      LATERAL (Reassignable Allotment)
                    </span>
                  ) : (
                    <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-slate-100 text-slate-700 border border-slate-300">
                      REGULAR (Fixed Allotment)
                    </span>
                  )}
                  {student.admissionCategory && (
                    <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                      Admission: {student.admissionCategory}
                    </span>
                  )}
                  {student.tuitionCategory && (
                    <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
                      Tuition: {student.tuitionCategory}
                    </span>
                  )}
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800">
                    <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                    {student.status || 'ACTIVE'}
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-y-1 gap-x-4 mt-2 text-xs text-slate-600">
                  {student.uniqueId && (
                    <span>
                      Unique ID: <strong className="font-mono text-slate-800">{student.uniqueId}</strong>
                    </span>
                  )}
                  {student.email && (
                    <span>
                      Institutional Email: <strong className="text-slate-800">{student.email}</strong>
                    </span>
                  )}
                  {student.placement && (
                    <span>
                      Section: <strong className="text-indigo-700">{student.placement.sectionName}</strong>
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Quick Context Badges & Actions */}
            <div className="flex flex-col sm:items-end justify-center pt-3 md:pt-0 border-t md:border-t-0 border-slate-100 gap-2">
              <div className="text-right">
                <div className="text-xs text-slate-500 font-medium">Academic Hierarchy</div>
                <div className="text-sm font-bold text-slate-900 mt-0.5">
                  {student.placement?.programName || 'AI & Machine Learning'}
                </div>
                <div className="text-xs text-indigo-600 font-medium mt-0.5">
                  {student.placement?.semesterName || '3rd Semester'} &bull; {student.placement?.academicYearName || '2025-2026'}
                </div>
              </div>
              {user?.role !== 'STUDENT' && (
                <div className="flex items-center gap-2">
                  {student.entryType === 'LATERAL' && (
                    <button
                      type="button"
                      onClick={() => setShowEditModal(true)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold shadow-2xs transition-colors cursor-pointer"
                    >
                      <Building className="w-3.5 h-3.5" /> Reassign Section
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => setShowEditModal(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold shadow-2xs transition-colors cursor-pointer"
                  >
                    <Edit className="w-3.5 h-3.5" /> Edit Student Data
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 2-Column Academic Overview Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          {/* Column 1: Identity & Placement Details */}
          <div className="lg:col-span-1 space-y-6">
            {/* Academic Placement Card */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <h2 className="text-sm font-bold text-slate-900 mb-4 pb-2 border-b border-slate-100 flex items-center gap-2">
                <Building className="w-4 h-4 text-indigo-600" />
                Institutional Placement
              </h2>

              <dl className="space-y-3 text-xs">
                <div>
                  <dt className="text-slate-400 font-medium">Department</dt>
                  <dd className="font-semibold text-slate-800 mt-0.5">
                    {student.placement?.departmentName || 'Computer Science and Engineering (CSE)'}
                  </dd>
                </div>
                <div>
                  <dt className="text-slate-400 font-medium">Program & Degree</dt>
                  <dd className="font-semibold text-slate-800 mt-0.5">
                    {student.placement?.degree || 'B.Tech'} - {student.placement?.programName || 'Artificial Intelligence & Machine Learning'}
                  </dd>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <dt className="text-slate-400 font-medium">Academic Year</dt>
                    <dd className="font-semibold text-slate-800 mt-0.5">
                      {student.placement?.academicYearName || '2025-2026'} (Year {student.placement?.yearNumber || 2})
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-400 font-medium">Semester</dt>
                    <dd className="font-semibold text-slate-800 mt-0.5">
                      {student.placement?.semesterName || '3rd Semester'}
                    </dd>
                  </div>
                </div>
                <div>
                  <dt className="text-slate-400 font-medium">Section</dt>
                  <dd className="font-semibold text-indigo-700 mt-0.5">
                    {student.placement?.sectionName || 'Section 1'}
                  </dd>
                </div>
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-100">
                  <div>
                    <dt className="text-slate-400 font-medium">Admission Category</dt>
                    <dd className="font-semibold text-slate-800 mt-0.5">
                      {student.admissionCategory || 'CONVENOR'}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-400 font-medium">Tuition Category</dt>
                    <dd className="font-semibold text-slate-800 mt-0.5">
                      {student.tuitionCategory || 'RTF'}
                    </dd>
                  </div>
                </div>
                <div className="pt-2 border-t border-slate-100">
                  <dt className="text-slate-400 font-medium flex items-center gap-1">
                    <Award className="w-3.5 h-3.5 text-teal-600" /> Designated Class Teacher
                  </dt>
                  <dd className="font-semibold text-slate-800 mt-0.5">
                    {student.classTeacherName || 'Not Assigned'}
                  </dd>
                  {student.classTeacherEmail && (
                    <dd className="text-slate-500 text-[11px]">{student.classTeacherEmail}</dd>
                  )}
                </div>
                <div className="pt-2 border-t border-slate-100">
                  <dt className="text-slate-400 font-medium flex items-center gap-1">
                    <HeartHandshake className="w-3.5 h-3.5 text-indigo-600" /> Assigned Counsellor / Mentor
                  </dt>
                  <dd className="font-semibold text-slate-800 mt-0.5">
                    {student.counsellorName || 'Not Assigned'}
                  </dd>
                  {student.counsellorEmail && (
                    <dd className="text-slate-500 text-[11px]">{student.counsellorEmail}</dd>
                  )}
                </div>
              </dl>
            </div>

            {/* Attendance Honest Empty State Card */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100 mb-3">
                <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-emerald-600" />
                  Attendance Summary
                </h2>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">
                  Pending Module
                </span>
              </div>
              <div className="p-4 rounded-lg bg-slate-50 border border-dashed border-slate-200 text-center">
                <p className="text-xs font-semibold text-slate-700">Attendance data not available yet</p>
                <p className="text-[11px] text-slate-500 mt-1">
                  Daily period-wise attendance and overall percentage tracking will appear once biometric/digital attendance logging begins.
                </p>
              </div>
            </div>

            {/* Academic Results Honest Empty State Card */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100 mb-3">
                <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <GraduationCap className="w-4 h-4 text-amber-600" />
                  Semester Results & SGPA
                </h2>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-600">
                  No Exam Records
                </span>
              </div>
              <div className="p-4 rounded-lg bg-slate-50 border border-dashed border-slate-200 text-center">
                <p className="text-xs font-semibold text-slate-700">No academic results recorded</p>
                <p className="text-[11px] text-slate-500 mt-1">
                  Midterm examinations and end-semester University marks/SGPA will be published here upon evaluation.
                </p>
              </div>
            </div>
          </div>

          {/* Column 2 & 3: Courses, Educational History, Timetable, and Counselling */}
          <div className="lg:col-span-2 space-y-6">
            {/* Current Registered Courses & Faculty Instructors */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <div>
                  <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <BookOpen className="w-4 h-4 text-indigo-600" />
                    Enrolled Courses & Assigned Faculty ({student.currentCourses?.length || 0})
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Official curriculum courses for {student.placement?.semesterName || 'Semester 3'}
                  </p>
                </div>
              </div>

              {!student.currentCourses || student.currentCourses.length === 0 ? (
                <div className="p-6 text-center text-slate-400 text-xs">
                  No registered subjects found for this semester.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3 w-20">Code</th>
                        <th className="py-2.5 px-3">Subject Name</th>
                        <th className="py-2.5 px-3 w-16 text-center">Type</th>
                        <th className="py-2.5 px-3 w-16 text-center">Credits</th>
                        <th className="py-2.5 px-3">Faculty Instructor</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {student.currentCourses.map((c) => (
                        <tr key={c.id} className="hover:bg-slate-50">
                          <td className="py-2.5 px-3 font-mono font-bold text-indigo-900">{c.code}</td>
                          <td className="py-2.5 px-3 font-semibold text-slate-800">{c.name}</td>
                          <td className="py-2.5 px-3 text-center">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              c.type === 'LAB' ? 'bg-purple-100 text-purple-800' : 'bg-slate-100 text-slate-700'
                            }`}>
                              {c.type}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-center font-bold text-slate-700">{c.credits}</td>
                          <td className="py-2.5 px-3">
                            <div className="font-medium text-slate-800">{c.facultyName || 'TBA'}</div>
                            {c.facultyEmail && (
                              <div className="text-[11px] text-slate-400">{c.facultyEmail}</div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Prior Educational History Table */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <div>
                  <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-teal-600" />
                    Prior Educational History
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Pre-admission qualifications verified during enrollment
                  </p>
                </div>
              </div>

              {!student.educationalHistory || student.educationalHistory.length === 0 ? (
                <div className="p-6 text-center text-slate-400 text-xs">
                  No prior educational qualifications recorded.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3">Qualification</th>
                        <th className="py-2.5 px-3">Board / University</th>
                        <th className="py-2.5 px-3">Institution</th>
                        <th className="py-2.5 px-3 w-16 text-center">Year</th>
                        <th className="py-2.5 px-3 text-right">Marks / %</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {student.educationalHistory.map((edu) => (
                        <tr key={edu.id} className="hover:bg-slate-50">
                          <td className="py-2.5 px-3 font-bold text-slate-800">
                            <span className="px-2 py-0.5 rounded bg-teal-50 text-teal-800 border border-teal-200">
                              {edu.qualificationType}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-slate-700">{edu.boardOrUniversity}</td>
                          <td className="py-2.5 px-3 text-slate-600">{edu.institutionName}</td>
                          <td className="py-2.5 px-3 text-center font-mono text-slate-700">{edu.completionYear}</td>
                          <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-800">
                            {edu.percentage != null
                              ? `${edu.percentage}%`
                              : edu.marksObtained != null
                              ? `${edu.marksObtained} / ${edu.maxMarks || ''}`
                              : edu.gradeOrCgpa || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Certificates & Verification Records (Phase 4) */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <div>
                  <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-indigo-600" />
                    Certificates & Document Verification ({certificates.length})
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Official original certificates submitted during admission and institutional verification status
                  </p>
                </div>
                {user?.role !== 'STUDENT' && (
                  <button
                    type="button"
                    onClick={handleOpenAddCert}
                    className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold border border-indigo-200 transition-colors cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" /> Record Certificate
                  </button>
                )}
              </div>

              {loadingCerts ? (
                <div className="p-6 text-center text-slate-500 text-xs">
                  <Loader2 className="w-5 h-5 animate-spin mx-auto text-indigo-600 mb-1" />
                  Loading certificate records...
                </div>
              ) : certificates.length === 0 ? (
                <div className="p-6 text-center text-slate-400 text-xs">
                  No certificate records registered yet for this student.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3">Certificate Type</th>
                        <th className="py-2.5 px-3">Cert / Roll No</th>
                        <th className="py-2.5 px-3">Issuing Authority & Year</th>
                        <th className="py-2.5 px-3 text-center">Status</th>
                        <th className="py-2.5 px-3">Verification Details</th>
                        {user?.role !== 'STUDENT' && (
                          <th className="py-2.5 px-3 text-right">Actions</th>
                        )}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {certificates.map((c) => (
                        <tr key={c.id} className="hover:bg-slate-50">
                          <td className="py-2.5 px-3 font-bold text-slate-800">
                            {c.certificateType.replace(/_/g, ' ')}
                          </td>
                          <td className="py-2.5 px-3 font-mono font-semibold text-slate-700">
                            {c.certificateNumber || '—'}
                          </td>
                          <td className="py-2.5 px-3 text-slate-600">
                            {c.issuingAuthority || '—'} {c.issueYear ? `(${c.issueYear})` : ''}
                          </td>
                          <td className="py-2.5 px-3 text-center">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                c.status === 'VERIFIED'
                                  ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                                  : c.status === 'SUBMITTED'
                                  ? 'bg-blue-100 text-blue-800 border border-blue-200'
                                  : c.status === 'RETURNED'
                                  ? 'bg-purple-100 text-purple-800 border border-purple-200'
                                  : 'bg-amber-100 text-amber-800 border border-amber-200'
                              }`}
                            >
                              {c.status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-slate-600">
                            {c.verifiedBy && (
                              <div className="text-[11px] font-medium text-slate-700">
                                Verified by: <span className="font-semibold">{c.verifiedBy}</span>
                              </div>
                            )}
                            {c.verifiedAt && (
                              <div className="text-[10px] text-slate-400">
                                {new Date(c.verifiedAt).toLocaleDateString()}
                              </div>
                            )}
                            {c.remarks && (
                              <div className="text-[11px] text-slate-500 italic mt-0.5">
                                "{c.remarks}"
                              </div>
                            )}
                            {!c.verifiedBy && !c.remarks && (
                              <span className="text-slate-400 text-[11px]">Pending verification</span>
                            )}
                          </td>
                          {user?.role !== 'STUDENT' && (
                            <td className="py-2.5 px-3 text-right">
                              <div className="flex items-center justify-end gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => handleOpenEditCert(c)}
                                  className="px-2 py-1 text-[11px] font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded border border-indigo-200 transition-colors cursor-pointer"
                                >
                                  Update
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDeleteCert(c.id)}
                                  className="p-1 text-slate-400 hover:text-red-600 rounded hover:bg-red-50 transition-colors cursor-pointer"
                                  title="Delete Certificate"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Section Timetable (Weekly Schedule) */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 mb-4 gap-2">
                <div>
                  <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-indigo-600" />
                    Section Timetable ({student.placement?.sectionName || 'Section 1'})
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    College hours: 09:30 AM – 04:30 PM &bull; Sorted chronologically Monday &rarr; Saturday
                  </p>
                </div>

                {/* Day Filter Pills */}
                <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
                  {daysOfWeek.map((day) => (
                    <button
                      key={day}
                      onClick={() => setSelectedDay(day)}
                      className={`px-2 py-1 rounded text-[11px] font-semibold transition-colors cursor-pointer shrink-0 ${
                        selectedDay === day
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      }`}
                    >
                      {day === 'ALL' ? 'All Days' : day.substring(0, 3)}
                    </button>
                  ))}
                </div>
              </div>

              {filteredTimetable.length === 0 ? (
                <div className="p-6 text-center text-slate-400 text-xs">
                  No classes scheduled for {selectedDay === 'ALL' ? 'this section' : selectedDay}.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3 w-24">Day</th>
                        <th className="py-2.5 px-3 w-16 text-center">Period</th>
                        <th className="py-2.5 px-3 w-32">Time</th>
                        <th className="py-2.5 px-3">Subject</th>
                        <th className="py-2.5 px-3 w-24">Room</th>
                        <th className="py-2.5 px-3">Instructor</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredTimetable.map((slot) => (
                        <tr key={slot.id} className="hover:bg-slate-50">
                          <td className="py-2.5 px-3 font-semibold text-slate-700">
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">
                              {slot.dayOfWeek}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-center font-bold text-slate-800">P{slot.periodNumber}</td>
                          <td className="py-2.5 px-3 font-mono text-[11px] text-slate-600">
                            {slot.startTime?.substring(0, 5)} - {slot.endTime?.substring(0, 5)}
                          </td>
                          <td className="py-2.5 px-3">
                            <span className="font-mono font-bold text-indigo-900 mr-1.5">{slot.subjectCode}</span>
                            <span className="text-slate-800 font-medium">{slot.subjectName}</span>
                          </td>
                          <td className="py-2.5 px-3">
                            <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
                              {slot.roomNumber || 'LH-301'}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-slate-700 font-medium">
                            {slot.facultyName || 'TBA'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Confidential Counselling History (Strictly Rendered ONLY if canViewCounselling === true) */}
            {student.canViewCounselling && (
              <div className="bg-white rounded-xl p-5 border border-indigo-200 shadow-2xs">
                <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                  <div className="flex items-center gap-2">
                    <HeartHandshake className="w-5 h-5 text-indigo-600" />
                    <div>
                      <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                        Mentorship & Counselling Records
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-900 border border-amber-200 flex items-center gap-1">
                          <Lock className="w-3 h-3 text-amber-700" /> Confidential
                        </span>
                      </h2>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Restricted to assigned counsellor, HOD, principal, and student.
                      </p>
                    </div>
                  </div>
                </div>

                {!student.counsellingRecords || student.counsellingRecords.length === 0 ? (
                  <div className="p-6 text-center text-slate-400 text-xs bg-slate-50 rounded-lg border border-dashed border-slate-200">
                    No counselling session records logged for this student yet.
                  </div>
                ) : (
                  <div className="space-y-3">
                    {student.counsellingRecords.map((rec) => (
                      <div key={rec.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50">
                        <div className="flex items-center justify-between mb-2">
                          <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">
                            {rec.recordType}
                          </span>
                          <span className="text-xs text-slate-500 flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5" /> Meeting Date: {rec.meetingDate}
                          </span>
                        </div>
                        <p className="text-xs text-slate-800 font-medium mb-1.5">
                          <strong>Remarks:</strong> {rec.remarks}
                        </p>
                        {rec.actionPlan && (
                          <p className="text-xs text-slate-600 mb-1.5">
                            <strong>Action Plan:</strong> {rec.actionPlan}
                          </p>
                        )}
                        <div className="mt-2 pt-2 border-t border-slate-200 text-[11px] text-slate-500 flex justify-between">
                          <span>Status: <strong className="uppercase text-slate-800">{rec.status}</strong></span>
                          {rec.followUpDate && <span>Follow-up: {rec.followUpDate}</span>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {showEditModal && student && (
          <StudentEditModal
            isOpen={showEditModal}
            onClose={() => setShowEditModal(false)}
            studentId={student.id || studentId!}
            initialData={student}
            metadata={metadata}
            onSaved={(updated) => {
              setStudent(updated);
            }}
          />
        )}

        {/* Certificate Add / Edit Modal */}
        {showCertModal && (
          <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-slate-900">
                      {editingCertId ? 'Update Certificate Record' : 'Record Submitted Certificate'}
                    </h3>
                    <p className="text-xs text-slate-500 font-mono">
                      Roll No: {student.registrationNumber}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowCertModal(false)}
                  className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {certModalError && (
                <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-red-500 shrink-0" />
                  <span>{certModalError}</span>
                </div>
              )}

              <form onSubmit={handleSaveCert} className="space-y-3.5 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Certificate Type *</label>
                  <select
                    value={certForm.certificateType}
                    onChange={(e) => setCertForm({ ...certForm, certificateType: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                  >
                    <option value="SSC_MARKS_MEMO">SSC (10th) Marks Memo</option>
                    <option value="INTERMEDIATE_MARKS_MEMO">Intermediate / +2 Marks Memo</option>
                    <option value="DIPLOMA_CERTIFICATE">Polytechnic Diploma Certificate</option>
                    <option value="TRANSFER_CERTIFICATE">Transfer Certificate (TC)</option>
                    <option value="STUDY_AND_CONDUCT">Study & Conduct Certificate</option>
                    <option value="EAPCET_RANK_CARD">EAPCET / ECET Rank Card</option>
                    <option value="CASTE_CERTIFICATE">Community / Caste Certificate</option>
                    <option value="INCOME_CERTIFICATE">Income / White Ration Card</option>
                    <option value="MIGRATION_CERTIFICATE">Migration Certificate</option>
                    <option value="SPECIAL_CATEGORY_CERTIFICATE">NCC / Sports / PH / CAP Certificate</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Certificate / Roll Number</label>
                    <input
                      type="text"
                      placeholder="e.g. 2205012345"
                      value={certForm.certificateNumber || ''}
                      onChange={(e) => setCertForm({ ...certForm, certificateNumber: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Issue Year</label>
                    <input
                      type="number"
                      placeholder="e.g. 2024"
                      value={certForm.issueYear || ''}
                      onChange={(e) => setCertForm({ ...certForm, issueYear: e.target.value ? Number(e.target.value) : undefined })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                    />
                  </div>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Issuing Authority / Board</label>
                  <input
                    type="text"
                    placeholder="e.g. BSEAP, BIEAP, SBTET, Revenue Department"
                    value={certForm.issuingAuthority || ''}
                    onChange={(e) => setCertForm({ ...certForm, issuingAuthority: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Verification Status *</label>
                  <select
                    value={certForm.status}
                    onChange={(e) => setCertForm({ ...certForm, status: e.target.value as CertificateStatus })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white font-semibold"
                  >
                    <option value="SUBMITTED">SUBMITTED (Original in Custody)</option>
                    <option value="VERIFIED">VERIFIED (Authenticated with Board)</option>
                    <option value="PENDING">PENDING (Deficiency / Awaited)</option>
                    <option value="RETURNED">RETURNED (Handed back to Student)</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Remarks / Verification Notes</label>
                  <input
                    type="text"
                    placeholder="e.g. Original submitted to office file; verified via online board portal"
                    value={certForm.remarks || ''}
                    onChange={(e) => setCertForm({ ...certForm, remarks: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowCertModal(false)}
                    className="px-4 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={savingCert}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
                  >
                    {savingCert && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    {editingCertId ? 'Save Changes' : 'Record Certificate'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
