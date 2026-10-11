import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { studentApi } from '../api/student.api';
import { facultyApi } from '../api/faculty.api';
import { isUserHod } from '../types/auth.types';
import type { StudentFullProfileDto, FacultyProfileDto } from '../types/auth.types';
import {
  User as UserIcon,
  Mail,
  Shield,
  Hash,
  CreditCard,
  BookOpen,
  ArrowLeft,
  LogOut,
  CheckCircle,
  Briefcase,
  GraduationCap,
  Award,
  HeartHandshake,
  Building,
  FileText,
  Loader2,
} from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [studentProfile, setStudentProfile] = useState<StudentFullProfileDto | null>(null);
  const [facultyProfile, setFacultyProfile] = useState<FacultyProfileDto | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);

  const isHod = isUserHod(user);

  useEffect(() => {
    let isMounted = true;

    async function loadEntityProfile() {
      if (!user) return;
      setLoadingProfile(true);
      try {
        if (user.role === 'STUDENT') {
          const sp = await studentApi.getMyProfile();
          if (isMounted) setStudentProfile(sp);
        } else if (user.role === 'FACULTY' || isHod || user.role === 'CLASS_TEACHER') {
          const fp = await facultyApi.getProfile();
          if (isMounted) setFacultyProfile(fp);
        }
      } catch (err) {
        console.warn('Could not load specialized profile details', err);
      } finally {
        if (isMounted) setLoadingProfile(false);
      }
    }

    loadEntityProfile();

    return () => {
      isMounted = false;
    };
  }, [user, isHod]);

  if (!user) return null;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const formatAadhaar = (num?: number | null) => {
    if (!num) return 'Not Provided';
    const s = num.toString().padStart(12, '0');
    return `${s.slice(0, 4)} ${s.slice(4, 8)} ${s.slice(8, 12)}`;
  };

  const formatApaar = (num?: number | null) => {
    if (!num) return 'Not Provided';
    const s = num.toString().padStart(12, '0');
    return `${s.slice(0, 4)} ${s.slice(4, 8)} ${s.slice(8, 12)}`;
  };

  const activeResps = user.responsibilities?.filter((r) => r.status === 'ACTIVE') || [];

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6 flex items-center justify-between">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-indigo-600 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>

          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 border border-red-200 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>

        {/* Profile Card */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden mb-6">
          {/* Header Banner */}
          <div className="bg-gradient-to-r from-indigo-700 via-indigo-800 to-slate-900 p-6 sm:p-8 text-white">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-xs flex items-center justify-center text-white text-2xl font-bold border border-white/30">
                  {user.fullName.charAt(0)}
                </div>
                <div>
                  <h1 className="text-2xl font-extrabold">{user.fullName}</h1>
                  <div className="flex flex-wrap items-center gap-2 mt-1.5">
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-white/20 text-white border border-white/30">
                      {isHod ? 'HEAD OF DEPARTMENT (HOD)' : user.role}
                    </span>
                    <span className="text-xs text-indigo-100 flex items-center gap-1">
                      <CheckCircle className="w-3.5 h-3.5 text-emerald-300" />
                      Account Active ({user.enabled ? 'Enabled' : 'Disabled'})
                    </span>
                  </div>
                </div>
              </div>

              {/* Entity Context Badge */}
              <div className="text-left sm:text-right pt-2 sm:pt-0 border-t sm:border-t-0 border-white/10">
                <div className="text-xs text-indigo-200">Institutional ID</div>
                <div className="text-base font-mono font-bold tracking-wide">
                  {user.registrationNumber || facultyProfile?.employeeNumber || user.uniqueId || 'CRRE-CSE'}
                </div>
              </div>
            </div>
          </div>

          <div className="p-6 sm:p-8 space-y-8">
            {/* Loading Indicator */}
            {loadingProfile && (
              <div className="flex items-center gap-2 text-xs text-slate-500 py-2">
                <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
                Loading detailed institutional credentials and academic placement...
              </div>
            )}

            {/* ===================== STUDENT SPECIALIZED VIEW ===================== */}
            {user.role === 'STUDENT' && studentProfile && (
              <div className="space-y-6">
                {/* Academic Placement Card */}
                <div>
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                    <Building className="w-4 h-4 text-indigo-600" />
                    Academic Placement & Enrollment Details
                  </h2>
                  <dl className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Department</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {studentProfile.departmentName}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Program</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {studentProfile.programName}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Term & Semester</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {studentProfile.semesterName} &bull; {studentProfile.academicYearName}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Assigned Section</dt>
                      <dd className="mt-1 text-sm font-bold text-indigo-700">
                        {studentProfile.sectionName}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-blue-50/50 border border-blue-100">
                      <dt className="text-xs text-blue-700 font-medium">Admission Category</dt>
                      <dd className="mt-1 text-sm font-bold text-blue-900">
                        {studentProfile.admissionCategory || 'CONVENOR'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-purple-50/50 border border-purple-100">
                      <dt className="text-xs text-purple-700 font-medium">Tuition Classification</dt>
                      <dd className="mt-1 text-sm font-bold text-purple-900">
                        {studentProfile.tuitionCategory || 'RTF (Fee Reimbursement)'}
                      </dd>
                    </div>
                  </dl>
                </div>

                {/* Mentorship & Support */}
                <div className="border-t border-slate-200 pt-6">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                    <HeartHandshake className="w-4 h-4 text-teal-600" />
                    Designated Academic Mentors
                  </h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80">
                      <div className="flex items-center gap-2 mb-2">
                        <Award className="w-4 h-4 text-teal-600" />
                        <span className="text-xs font-bold text-slate-800">Class Teacher</span>
                      </div>
                      <div className="text-sm font-semibold text-slate-900">
                        {studentProfile.classTeacherName || 'Not Assigned'}
                      </div>
                      {studentProfile.classTeacherEmail && (
                        <div className="text-xs text-slate-500 mt-0.5">
                          {studentProfile.classTeacherEmail}
                        </div>
                      )}
                    </div>

                    <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80">
                      <div className="flex items-center gap-2 mb-2">
                        <HeartHandshake className="w-4 h-4 text-indigo-600" />
                        <span className="text-xs font-bold text-slate-800">Counsellor / Proctor</span>
                      </div>
                      <div className="text-sm font-semibold text-slate-900">
                        {studentProfile.counsellorName || 'Not Assigned'}
                      </div>
                      {studentProfile.counsellorEmail && (
                        <div className="text-xs text-slate-500 mt-0.5">
                          {studentProfile.counsellorEmail}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Prior Educational History Table */}
                <div className="border-t border-slate-200 pt-6">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-indigo-600" />
                    Prior Educational Qualifications ({studentProfile.educationalHistory.length})
                  </h2>

                  {studentProfile.educationalHistory.length === 0 ? (
                    <p className="text-xs text-slate-400 italic">No prior qualification records found.</p>
                  ) : (
                    <div className="overflow-x-auto border border-slate-200 rounded-xl">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                          <tr>
                            <th className="py-2.5 px-3">Qualification</th>
                            <th className="py-2.5 px-3">Board / University</th>
                            <th className="py-2.5 px-3">Institution Name</th>
                            <th className="py-2.5 px-3 text-center">Year</th>
                            <th className="py-2.5 px-3 text-right">Percentage / Marks</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {studentProfile.educationalHistory.map((edu) => (
                            <tr key={edu.id} className="hover:bg-slate-50/60">
                              <td className="py-2.5 px-3 font-bold text-slate-800">
                                <span className="px-2 py-0.5 rounded bg-teal-50 text-teal-800 border border-teal-200 font-mono">
                                  {edu.qualificationType}
                                </span>
                              </td>
                              <td className="py-2.5 px-3 text-slate-700">{edu.boardOrUniversity}</td>
                              <td className="py-2.5 px-3 text-slate-600">{edu.institutionName}</td>
                              <td className="py-2.5 px-3 text-center font-mono text-slate-700">
                                {edu.completionYear}
                              </td>
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
              </div>
            )}

            {/* ===================== FACULTY SPECIALIZED VIEW ===================== */}
            {(user.role === 'FACULTY' || isHod || user.role === 'CLASS_TEACHER') && (
              <div className="space-y-6">
                <div>
                  <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                    <GraduationCap className="w-4 h-4 text-indigo-600" />
                    Faculty Designation & Academic Credentials
                  </h2>

                  <dl className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Employee Number</dt>
                      <dd className="mt-1 text-sm font-mono font-bold text-indigo-700">
                        {facultyProfile?.employeeNumber || user.registrationNumber || 'CRRE-FAC'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Department</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {facultyProfile?.departmentName || 'Computer Science and Engineering (CSE)'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Designation</dt>
                      <dd className="mt-1 text-sm font-bold text-slate-900">
                        {facultyProfile?.designation || (isHod ? 'Professor & Head of Department' : 'Assistant Professor')}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Highest Qualification</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {facultyProfile?.qualification || 'M.Tech, Ph.D'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Specialization / Domain</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {facultyProfile?.specialization || 'Computer Science & Engineering'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Professional Experience</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {facultyProfile?.professionalExperience || '10+ Years in Teaching & Research'}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                      <dt className="text-xs text-slate-500 font-medium">Institutional Contact</dt>
                      <dd className="mt-1 text-sm font-semibold text-slate-900">
                        {facultyProfile?.institutionalContact || user.email}
                      </dd>
                    </div>

                    <div className="p-3.5 rounded-lg bg-emerald-50/50 border border-emerald-100">
                      <dt className="text-xs text-emerald-700 font-medium">Active Status</dt>
                      <dd className="mt-1 text-sm font-bold text-emerald-900">
                        {facultyProfile?.activeStatus ? 'Active Faculty Duty' : 'Active Faculty Duty'}
                      </dd>
                    </div>
                  </dl>
                </div>
              </div>
            )}

            {/* Account Information (Common) */}
            <div className="border-t border-slate-200 pt-6">
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
                Account Credentials & System Information
              </h2>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <UserIcon className="w-3.5 h-3.5 text-slate-400" />
                    Username
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900 font-mono">
                    {user.username}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Mail className="w-3.5 h-3.5 text-slate-400" />
                    Email Address
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900">{user.email}</dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Shield className="w-3.5 h-3.5 text-slate-400" />
                    Primary Role
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900">
                    {user.role} {isHod && '(with Active HOD Responsibility)'}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Hash className="w-3.5 h-3.5 text-slate-400" />
                    System UUID
                  </dt>
                  <dd className="mt-1 text-xs font-mono font-semibold text-slate-700 truncate">
                    {user.id}
                  </dd>
                </div>
              </dl>
            </div>

            {/* Active Institutional Responsibilities */}
            <div className="border-t border-slate-200 pt-6">
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4 flex items-center gap-2">
                <Briefcase className="w-4 h-4 text-indigo-600" />
                Active Institutional Responsibilities ({activeResps.length})
              </h2>

              {activeResps.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {activeResps.map((r) => (
                    <div
                      key={r.id}
                      className="p-3.5 rounded-lg bg-indigo-50/50 border border-indigo-100 text-xs"
                    >
                      <div className="flex items-center justify-between font-bold text-slate-900 mb-1">
                        <span>{r.type.replace('_', ' ')}</span>
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 font-bold">
                          ACTIVE
                        </span>
                      </div>
                      {r.subjectName && (
                        <div className="text-slate-600">
                          Subject: <strong className="text-slate-800">{r.subjectName}</strong> ({r.subjectCode})
                        </div>
                      )}
                      {r.sectionName && (
                        <div className="text-slate-600">
                          Section: <strong className="text-slate-800">{r.sectionName}</strong>
                        </div>
                      )}
                      {r.departmentName && (
                        <div className="text-slate-600">
                          Department: <strong className="text-slate-800">{r.departmentName}</strong>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-500 italic">No departmental responsibilities assigned.</p>
              )}
            </div>

            {/* Institutional & Government Identifiers */}
            <div className="border-t border-slate-200 pt-6">
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-4">
                Institutional & Government Identifiers
              </h2>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium">
                    College Registration Number
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {user.registrationNumber || <span className="text-slate-400 font-normal">N/A</span>}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium">
                    Institutional Unique ID
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {user.uniqueId || <span className="text-slate-400 font-normal">N/A</span>}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-1.5">
                    <CreditCard className="w-3.5 h-3.5 text-slate-400" />
                    Aadhaar Number (12 Digits)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {formatAadhaar(user.aadharNumber)}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5 text-slate-400" />
                    APAAR ID (12 Digits)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {formatApaar(user.apaarNumber)}
                  </dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
