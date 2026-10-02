import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import type { AcademicMetadata, ResponsibilitySummary } from '../types/auth.types';
import {
  ShieldCheck,
  User,
  ArrowRight,
  Building,
  GraduationCap,
  Users,
  Calendar,
  BookOpen,
  Award,
  Layers,
  Sparkles,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [metadata, setMetadata] = useState<AcademicMetadata | null>(null);
  const [teaching, setTeaching] = useState<ResponsibilitySummary[]>([]);

  useEffect(() => {
    async function loadData() {
      try {
        const meta = await academicApi.getMetadata();
        setMetadata(meta);

        if (
          user?.role === 'FACULTY' ||
          user?.role === 'CLASS_TEACHER' ||
          user?.role === 'HOD'
        ) {
          try {
            const myTeaching = await academicApi.getMyTeaching();
            setTeaching(myTeaching);
          } catch {
            // ignore if not faculty
          }
        }
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      }
    }
    loadData();
  }, [user]);

  if (!user) return null;

  const isClassTeacher =
    user.role === 'CLASS_TEACHER' ||
    user.responsibilities?.some(
      (r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE'
    );

  const canManage =
    user.role === 'ADMIN' || user.role === 'PRINCIPAL' || user.role === 'HOD';

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Institutional Hierarchy Breadcrumb */}
        <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-2xs mb-6 text-xs text-slate-600 flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1 font-semibold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
            <Building className="w-3.5 h-3.5" /> CRRE
          </span>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-slate-800">
            Dept. of Computer Science & Engineering (CSE)
          </span>
          <span className="text-slate-400">/</span>
          <span className="font-semibold text-indigo-900">
            B.Tech AI & Machine Learning
          </span>
          <span className="text-slate-400">/</span>
          <span className="text-slate-700">2nd Year (2025-2026)</span>
          <span className="text-slate-400">/</span>
          <span className="text-slate-700">3rd Semester</span>
          <span className="text-slate-400">/</span>
          <span className="inline-flex items-center gap-1 font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
            Section 1 (66 Students)
          </span>
        </div>

        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-indigo-900 rounded-2xl p-6 sm:p-8 text-white shadow-md mb-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-2 mb-3">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-semibold border border-emerald-500/30">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  Session Auth Active (JSESSIONID)
                </span>
                {isClassTeacher && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-teal-500/20 text-teal-300 text-xs font-semibold border border-teal-500/30">
                    <Award className="w-3.5 h-3.5" />
                    Class Teacher: CSE AI&ML Section 1
                  </span>
                )}
                {user.role === 'PRINCIPAL' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-500/20 text-rose-300 text-xs font-semibold border border-rose-500/30">
                    <Building className="w-3.5 h-3.5" />
                    Institutional Authority: CRRE
                  </span>
                )}
                {user.role === 'HOD' && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/20 text-amber-300 text-xs font-semibold border border-amber-500/30">
                    <Layers className="w-3.5 h-3.5" />
                    Department Head: CSE
                  </span>
                )}
              </div>

              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                Welcome, {user.fullName}
              </h1>
              <p className="mt-1 text-sm sm:text-base text-indigo-200">
                Logged in as <span className="font-semibold text-white">{user.role}</span> &bull;{' '}
                <span className="font-mono text-indigo-100">
                  {user.registrationNumber || user.uniqueId || user.username}
                </span>
              </p>
            </div>

            <div className="flex items-center gap-3">
              <Link
                to="/profile"
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-white/10 hover:bg-white/20 text-white font-semibold text-sm transition-colors border border-white/20 backdrop-blur-xs"
              >
                <User className="w-4 h-4" />
                Profile
              </Link>
              {canManage && (
                <Link
                  to="/users"
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-indigo-500 hover:bg-indigo-600 text-white font-semibold text-sm transition-colors shadow-sm"
                >
                  <Users className="w-4 h-4" />
                  Manage Users
                  <ArrowRight className="w-4 h-4" />
                </Link>
              )}
            </div>
          </div>
        </div>

        {/* Prototype Stats Bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-8">
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
            <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
              <span>Department</span>
              <Building className="w-4 h-4 text-indigo-500" />
            </div>
            <div className="text-2xl font-bold text-slate-900">CSE</div>
            <div className="text-xs text-slate-500 mt-1">Multi-program enabled</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs">
            <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
              <span>Program</span>
              <GraduationCap className="w-4 h-4 text-indigo-500" />
            </div>
            <div className="text-2xl font-bold text-slate-900">AI & ML</div>
            <div className="text-xs text-slate-500 mt-1">2nd Year / 3rd Sem</div>
          </div>

          <Link
            to="/students"
            className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-300 hover:shadow-xs transition-all group"
          >
            <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
              <span>Enrolled Students</span>
              <Users className="w-4 h-4 text-emerald-600 group-hover:scale-110 transition-transform" />
            </div>
            <div className="text-2xl font-bold text-slate-900">66</div>
            <div className="text-xs text-emerald-600 font-medium mt-1 flex items-center gap-1">
              Section 1 (25B81A4201-66) &rarr;
            </div>
          </Link>

          <Link
            to="/timetable"
            className="bg-white p-5 rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-300 hover:shadow-xs transition-all group"
          >
            <div className="text-slate-500 text-xs font-medium uppercase tracking-wider mb-1 flex items-center justify-between">
              <span>Curriculum & Slots</span>
              <Calendar className="w-4 h-4 text-indigo-600 group-hover:scale-110 transition-transform" />
            </div>
            <div className="text-2xl font-bold text-slate-900">9 Subjects</div>
            <div className="text-xs text-indigo-600 font-medium mt-1 flex items-center gap-1">
              7 Daily Periods (09:30-04:30) &rarr;
            </div>
          </Link>
        </div>

        {/* Dynamic Role Sections */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          {/* Main Content Area (2 cols) */}
          <div className="lg:col-span-2 space-y-6">
            {/* Student View Specific Card */}
            {user.role === 'STUDENT' && (
              <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                      <GraduationCap className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-slate-900">Academic Profile</h3>
                      <p className="text-xs text-slate-500">B.Tech CSE (AI&ML) &bull; Section 1</p>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-700 border border-emerald-200">
                    Active Student
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
                    <div className="text-slate-500 font-medium">Roll / Reg Number</div>
                    <div className="text-sm font-bold text-slate-900 font-mono mt-0.5">
                      {user.registrationNumber || '25B81A4201'}
                    </div>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
                    <div className="text-slate-500 font-medium">Institutional Unique ID</div>
                    <div className="text-sm font-bold text-slate-900 font-mono mt-0.5">
                      {user.uniqueId || 'CRRE20244201'}
                    </div>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
                    <div className="text-slate-500 font-medium">Designated Class Teacher</div>
                    <div className="text-sm font-bold text-indigo-700 mt-0.5">
                      CH. Rama Lakshmi (DMGT)
                    </div>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-lg border border-slate-100">
                    <div className="text-slate-500 font-medium">Academic Counsellor</div>
                    <div className="text-sm font-bold text-slate-900 mt-0.5">
                      CSE AI&ML Counsellor
                    </div>
                  </div>
                </div>

                <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-xs text-slate-500">
                    Semester 3 Curriculum: 6 Theory + 3 Labs (22.5 Credits)
                  </span>
                  <Link
                    to="/timetable"
                    className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                  >
                    View Timetable &rarr;
                  </Link>
                </div>
              </div>
            )}

            {/* Faculty / Class Teacher Teaching Card */}
            {(user.role === 'FACULTY' || isClassTeacher) && (
              <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold">
                      <BookOpen className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-slate-900">Teaching & Academic Load</h3>
                      <p className="text-xs text-slate-500">Current active assignments</p>
                    </div>
                  </div>
                  {isClassTeacher && (
                    <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-teal-100 text-teal-700 border border-teal-200">
                      Section 1 Class Teacher
                    </span>
                  )}
                </div>

                {teaching.length > 0 ? (
                  <div className="space-y-3">
                    {teaching.map((t) => (
                      <div
                        key={t.id}
                        className="p-3.5 rounded-lg border border-slate-200 hover:border-indigo-300 transition-colors bg-slate-50 flex items-center justify-between"
                      >
                        <div>
                          <div className="font-semibold text-slate-900 text-sm">
                            {t.subjectName || 'Discrete Mathematics & Graph Theory'}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            Code: <span className="font-mono">{t.subjectCode || '23A30501'}</span> &bull; Section:{' '}
                            <span className="font-semibold text-slate-700">{t.sectionName || 'Section 1'}</span>
                          </div>
                        </div>
                        <span className="text-xs px-2.5 py-1 rounded font-semibold bg-emerald-100 text-emerald-700">
                          3.0 Credits
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="p-4 bg-slate-50 rounded-lg text-xs text-slate-600">
                    <p className="font-semibold text-slate-800">
                      Confirmed Course: Discrete Mathematics & Graph Theory (DMGT)
                    </p>
                    <p className="mt-1">
                      Faculty: CH. Rama Lakshmi &bull; Section 1 &bull; 3.0 Credits (Active)
                    </p>
                  </div>
                )}

                <div className="mt-5 pt-4 border-t border-slate-100 flex items-center justify-between">
                  <Link
                    to="/students"
                    className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                  >
                    View Section 1 Students (66) &rarr;
                  </Link>
                  <Link
                    to="/timetable"
                    className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                  >
                    View Weekly Schedule &rarr;
                  </Link>
                </div>
              </div>
            )}

            {/* Curriculum Subjects Table */}
            <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    B.Tech AI&ML 3rd Semester Subjects (9)
                  </h3>
                  <p className="text-xs text-slate-500">6 Theory Courses + 3 Laboratory Courses</p>
                </div>
                <span className="text-xs font-semibold px-2 py-0.5 bg-indigo-50 text-indigo-700 rounded border border-indigo-200">
                  Total: 22.5 Credits
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-semibold border-y border-slate-200">
                    <tr>
                      <th className="py-2.5 px-3">Code</th>
                      <th className="py-2.5 px-3">Subject Name</th>
                      <th className="py-2.5 px-3">Type</th>
                      <th className="py-2.5 px-3 text-right">Credits</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {(metadata?.subjects || [
                      { id: '1', code: '23A30501', name: 'Discrete Mathematics & Graph Theory (DMGT)', type: 'THEORY', credits: 3.0 },
                      { id: '2', code: '23A30502', name: 'Advanced Data Structures & Algorithms', type: 'THEORY', credits: 3.0 },
                      { id: '3', code: '23A30503', name: 'Computer Organization & Architecture', type: 'THEORY', credits: 3.0 },
                      { id: '4', code: '23A30504', name: 'Database Management Systems', type: 'THEORY', credits: 3.0 },
                      { id: '5', code: '23A30505', name: 'Artificial Intelligence Principles & Tech', type: 'THEORY', credits: 3.0 },
                      { id: '6', code: '23A30506', name: 'Managerial Economics & Financial Analysis', type: 'THEORY', credits: 3.0 },
                      { id: '7', code: '23A30507', name: 'Advanced Data Structures Lab', type: 'LAB', credits: 1.5 },
                      { id: '8', code: '23A30508', name: 'Database Management Systems Lab', type: 'LAB', credits: 1.5 },
                      { id: '9', code: '23A30509', name: 'AI Programming & Tools Lab', type: 'LAB', credits: 1.5 },
                    ]).map((sub) => (
                      <tr key={sub.id} className="hover:bg-slate-50">
                        <td className="py-2 px-3 font-mono font-semibold text-slate-800">
                          {sub.code}
                        </td>
                        <td className="py-2 px-3 font-medium text-slate-900">{sub.name}</td>
                        <td className="py-2 px-3">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                              sub.type === 'LAB'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-blue-100 text-blue-800'
                            }`}
                          >
                            {sub.type}
                          </span>
                        </td>
                        <td className="py-2 px-3 text-right font-mono font-semibold text-slate-800">
                          {sub.credits.toFixed(1)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Sidebar Area (1 col) */}
          <div className="space-y-6">
            {/* Active User Responsibilities Widget */}
            <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
                <h3 className="text-sm font-bold text-slate-900">Active Responsibilities</h3>
                <span className="text-[10px] bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded font-semibold">
                  Lifecycle Active
                </span>
              </div>

              {user.responsibilities && user.responsibilities.length > 0 ? (
                <div className="space-y-2.5">
                  {user.responsibilities
                    .filter((r) => r.status === 'ACTIVE')
                    .map((r) => (
                      <div
                        key={r.id}
                        className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs"
                      >
                        <div className="flex items-center justify-between font-semibold text-slate-900">
                          <span>{r.type.replace('_', ' ')}</span>
                          <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-100 text-emerald-800 font-bold">
                            ACTIVE
                          </span>
                        </div>
                        {r.departmentName && (
                          <div className="text-slate-500 mt-1">
                            Dept: <span className="font-medium text-slate-700">{r.departmentName}</span>
                          </div>
                        )}
                        {r.programName && (
                          <div className="text-slate-500">
                            Prog: <span className="font-medium text-slate-700">{r.programName}</span>
                          </div>
                        )}
                        {r.subjectName && (
                          <div className="text-slate-500">
                            Subject: <span className="font-medium text-slate-700">{r.subjectName}</span>
                          </div>
                        )}
                        {r.sectionName && (
                          <div className="text-slate-500">
                            Section: <span className="font-medium text-slate-700">{r.sectionName}</span>
                          </div>
                        )}
                      </div>
                    ))}
                </div>
              ) : (
                <p className="text-xs text-slate-500">
                  {user.role === 'ADMIN'
                    ? 'Superuser: Global administrative authority across CRRE.'
                    : 'Standard student or single-role access.'}
                </p>
              )}

              {canManage && (
                <Link
                  to="/users"
                  className="mt-4 block text-center py-2 px-3 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg text-xs font-semibold transition-colors"
                >
                  Manage Faculty Assignments &rarr;
                </Link>
              )}
            </div>

            {/* Timetable Schedule Card */}
            <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-2xs">
              <div className="flex items-center gap-3 mb-3">
                <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                  <Calendar className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Daily College Schedule</h3>
                  <p className="text-xs text-slate-500">09:30 AM to 04:30 PM</p>
                </div>
              </div>
              <div className="space-y-1.5 text-xs text-slate-600">
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 1</span>
                  <span className="font-mono text-slate-800">09:30 - 10:20 AM</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 2</span>
                  <span className="font-mono text-slate-800">10:20 - 11:10 AM</span>
                </div>
                <div className="flex justify-between py-1 bg-amber-50 px-1 rounded text-amber-800 font-medium">
                  <span>Morning Break</span>
                  <span className="font-mono">11:10 - 11:30 AM</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 3</span>
                  <span className="font-mono text-slate-800">11:30 - 12:20 PM</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 4</span>
                  <span className="font-mono text-slate-800">12:20 - 01:10 PM</span>
                </div>
                <div className="flex justify-between py-1 bg-amber-50 px-1 rounded text-amber-800 font-medium">
                  <span>Lunch Interval</span>
                  <span className="font-mono">01:10 - 02:10 PM</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 5</span>
                  <span className="font-mono text-slate-800">02:10 - 03:00 PM</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Period 6</span>
                  <span className="font-mono text-slate-800">03:00 - 03:50 PM</span>
                </div>
                <div className="flex justify-between py-1">
                  <span>Period 7</span>
                  <span className="font-mono text-slate-800">03:50 - 04:30 PM</span>
                </div>
              </div>

              <Link
                to="/timetable"
                className="mt-4 block text-center py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-lg text-xs font-semibold transition-colors"
              >
                View Full Section Grid &rarr;
              </Link>
            </div>

            {/* Institutional Compliance Card */}
            <div className="bg-indigo-50 rounded-xl p-5 border border-indigo-100">
              <div className="flex items-center gap-2 mb-2 text-indigo-900 font-bold text-xs">
                <Sparkles className="w-4 h-4 text-indigo-600" />
                V2 Prototype Prototype Scope
              </div>
              <p className="text-[11px] text-indigo-700 leading-relaxed">
                CSE Department AI&ML prototype demonstrating Session Authentication, Downward
                Authority, Mutable Responsibilities, 66 Section 1 Students, and Master Timetable.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
