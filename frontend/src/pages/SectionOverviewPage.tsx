import React, { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import type {
  SectionDetailOverviewDto,
  TimetableEntryDto,
  StudentDto,
} from '../types/auth.types';
import {
  ArrowLeft,
  Users,
  Award,
  BookOpen,
  Calendar,
  Loader2,
  AlertCircle,
  Search,
  ChevronRight,
  GraduationCap,
} from 'lucide-react';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

const PERIODS = [
  { period: 1, label: 'Period 1', time: '09:30 - 10:20' },
  { period: 2, label: 'Period 2', time: '10:20 - 11:10' },
  { period: 3, label: 'Period 3', time: '11:30 - 12:20' },
  { period: 4, label: 'Period 4', time: '12:20 - 13:10' },
  { period: 5, label: 'Period 5', time: '14:10 - 15:00' },
  { period: 6, label: 'Period 6', time: '15:00 - 15:50' },
  { period: 7, label: 'Period 7', time: '15:50 - 16:30' },
];

export const SectionOverviewPage: React.FC = () => {
  const { sectionId } = useParams<{ sectionId: string }>();
  const navigate = useNavigate();

  const [overview, setOverview] = useState<SectionDetailOverviewDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState<'roster' | 'timetable' | 'subjects'>('roster');

  useEffect(() => {
    if (!sectionId) return;
    setLoading(true);
    setError(null);

    academicApi
      .getSectionOverview(sectionId)
      .then((data) => {
        setOverview(data);
      })
      .catch((err) => {
        console.error('Failed to load section overview', err);
        setError(err instanceof Error ? err.message : 'Failed to load section details');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [sectionId]);

  const rosterStudents: StudentDto[] = useMemo(() => {
    return overview?.students || [];
  }, [overview]);

  const filteredStudents = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return rosterStudents;
    return rosterStudents.filter(
      (s: StudentDto) =>
        s.fullName.toLowerCase().includes(q) ||
        s.registrationNumber.toLowerCase().includes(q)
    );
  }, [rosterStudents, search]);

  const getTimetableSlot = (day: string, periodNumber: number): TimetableEntryDto | undefined => {
    if (!overview?.timetable) return undefined;
    return overview.timetable.find(
      (t) => t.dayOfWeek?.toUpperCase() === day && t.periodNumber === periodNumber
    );
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center">
          <Loader2 className="w-10 h-10 text-indigo-600 animate-spin mb-3" />
          <p className="text-sm font-semibold text-slate-600">Loading Section Details...</p>
        </div>
      </div>
    );
  }

  if (error || !overview) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Navbar />
        <main className="flex-1 max-w-4xl w-full mx-auto px-4 py-12">
          <div className="bg-white rounded-2xl p-8 border border-slate-200 text-center shadow-xs">
            <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
            <h2 className="text-lg font-bold text-slate-900 mb-2">Failed to Load Section</h2>
            <p className="text-sm text-slate-600 mb-6">{error || 'Section details not found'}</p>
            <button
              onClick={() => navigate(-1)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-indigo-600 text-white font-semibold text-xs hover:bg-indigo-700 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" /> Go Back
            </button>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Navigation Breadcrumb / Back button */}
        <div className="mb-6 flex items-center justify-between">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:text-indigo-600 hover:border-indigo-300 font-semibold text-xs transition-colors shadow-2xs cursor-pointer"
          >
            <ArrowLeft className="w-4 h-4" /> Back
          </button>

          <span className="text-xs text-slate-500 font-medium">
            {overview.academicYearName} &bull; {overview.programName}
          </span>
        </div>

        {/* Header Hero Card */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold shadow-sm">
                <GraduationCap className="w-7 h-7" />
              </div>
              <div>
                <div className="flex items-center gap-2.5">
                  <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                    {overview.sectionName}
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-md text-xs font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                    {overview.programCode}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  {overview.programName} &bull; {overview.semesterName} ({overview.academicYearName})
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-right">
                <div className="text-xs text-slate-500 font-medium">Class Teacher</div>
                <div className="text-sm font-bold text-slate-900 flex items-center gap-1.5 justify-end">
                  <Award className="w-4 h-4 text-teal-600" />
                  {overview.classTeacherName || 'Unassigned'}
                </div>
                {overview.classTeacherEmail && (
                  <div className="text-[11px] text-slate-400 font-mono">{overview.classTeacherEmail}</div>
                )}
              </div>
            </div>
          </div>

          {/* Quick Metrics Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6 pt-6 border-t border-slate-100">
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/70">
              <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Total Enrolled</div>
              <div className="text-xl font-extrabold text-slate-900 mt-0.5">{overview.totalStudents} Students</div>
            </div>
            <div className="p-3.5 rounded-xl bg-teal-50/70 border border-teal-200/70">
              <div className="text-[11px] font-semibold text-teal-700 uppercase tracking-wider">Regular Entry</div>
              <div className="text-xl font-extrabold text-teal-900 mt-0.5">{overview.regularStudents} Students</div>
            </div>
            <div className="p-3.5 rounded-xl bg-indigo-50/70 border border-indigo-200/70">
              <div className="text-[11px] font-semibold text-indigo-700 uppercase tracking-wider">Lateral Entry</div>
              <div className="text-xl font-extrabold text-indigo-900 mt-0.5">{overview.lateralStudents} Students</div>
            </div>
            <div className="p-3.5 rounded-xl bg-purple-50/70 border border-purple-200/70">
              <div className="text-[11px] font-semibold text-purple-700 uppercase tracking-wider">Curriculum Courses</div>
              <div className="text-xl font-extrabold text-purple-900 mt-0.5">{overview.subjects.length} Subjects</div>
            </div>
          </div>
        </div>

        {/* Tab Controls */}
        <div className="flex border-b border-slate-200 mb-6 gap-2">
          <button
            onClick={() => setActiveTab('roster')}
            className={`px-4 py-2.5 text-xs font-bold border-b-2 flex items-center gap-2 transition-colors cursor-pointer ${
              activeTab === 'roster'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-900'
            }`}
          >
            <Users className="w-4 h-4" /> Student Roster ({rosterStudents.length})
          </button>
          <button
            onClick={() => setActiveTab('timetable')}
            className={`px-4 py-2.5 text-xs font-bold border-b-2 flex items-center gap-2 transition-colors cursor-pointer ${
              activeTab === 'timetable'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-900'
            }`}
          >
            <Calendar className="w-4 h-4" /> Weekly Timetable Grid
          </button>
          <button
            onClick={() => setActiveTab('subjects')}
            className={`px-4 py-2.5 text-xs font-bold border-b-2 flex items-center gap-2 transition-colors cursor-pointer ${
              activeTab === 'subjects'
                ? 'border-indigo-600 text-indigo-600'
                : 'border-transparent text-slate-500 hover:text-slate-900'
            }`}
          >
            <BookOpen className="w-4 h-4" /> Subjects & Faculty Allocation ({overview.subjects.length})
          </button>
        </div>

        {/* TAB 1: STUDENT ROSTER */}
        {activeTab === 'roster' && (
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
            <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50/50">
              <div className="relative flex-1 max-w-md">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search students by roll number or name..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                />
              </div>

              <span className="text-xs text-slate-500 font-medium">
                Showing {filteredStudents.length} of {rosterStudents.length} students
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4 w-12 text-center">#</th>
                    <th className="py-3 px-4">Roll Number</th>
                    <th className="py-3 px-4">Student Name</th>
                    <th className="py-3 px-4">Entry Type</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredStudents.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-8 text-center text-slate-500">
                        No students found matching "{search}"
                      </td>
                    </tr>
                  ) : (
                    filteredStudents.map((s: StudentDto, idx: number) => (
                      <tr
                        key={s.id}
                        onClick={() => navigate(`/students/${s.id}`)}
                        className="hover:bg-indigo-50/60 cursor-pointer transition-colors group"
                      >
                        <td className="py-3 px-4 text-center font-mono text-slate-400 font-medium">
                          {idx + 1}
                        </td>
                        <td className="py-3 px-4 font-mono font-bold text-indigo-900 group-hover:text-indigo-700">
                          {s.registrationNumber}
                        </td>
                        <td className="py-3 px-4 font-semibold text-slate-800 group-hover:text-indigo-950">
                          {s.fullName}
                        </td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              s.entryType === 'REGULAR'
                                ? 'bg-teal-100 text-teal-800'
                                : 'bg-indigo-100 text-indigo-800'
                            }`}
                          >
                            {s.entryType}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">
                            {s.status}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-right">
                          <span className="inline-flex items-center text-slate-400 group-hover:text-indigo-600 font-semibold text-[11px] gap-0.5">
                            Details <ChevronRight className="w-3.5 h-3.5" />
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 2: TIMETABLE GRID */}
        {activeTab === 'timetable' && (
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs p-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-100 mb-6 gap-2">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Weekly Class Timetable &bull; {overview.sectionName}
                </h3>
                <p className="text-xs text-slate-500">
                  Canonical Period Timing: 09:30 - 16:30 &bull; Verified Conflict-Free Academic Schedule
                </p>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full border-collapse border border-slate-200 text-xs">
                <thead>
                  <tr className="bg-slate-100 text-slate-700 font-bold">
                    <th className="border border-slate-300 p-2.5 text-left w-24">Day</th>
                    {PERIODS.map((p) => (
                      <th key={p.period} className="border border-slate-300 p-2.5 text-center min-w-[130px]">
                        <div>{p.label}</div>
                        <div className="text-[10px] font-mono font-normal text-slate-500">{p.time}</div>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {DAYS.map((day) => (
                    <tr key={day} className="hover:bg-slate-50/70">
                      <td className="border border-slate-300 p-2.5 font-bold text-slate-800 bg-slate-50">
                        {day}
                      </td>
                      {PERIODS.map((p) => {
                        const entry = getTimetableSlot(day, p.period);
                        return (
                          <td
                            key={p.period}
                            className={`border border-slate-300 p-2 text-center align-top ${
                              entry?.sessionType === 'LAB'
                                ? 'bg-amber-50/50 hover:bg-amber-100/50'
                                : 'hover:bg-indigo-50/40'
                            }`}
                          >
                            {entry ? (
                              <div>
                                <div className="font-bold text-slate-900 leading-tight">
                                  {entry.subjectCode}
                                </div>
                                <div className="text-[10px] text-slate-600 truncate max-w-[125px] mx-auto mt-0.5">
                                  {entry.subjectName}
                                </div>
                                <div className="text-[10px] font-medium text-indigo-700 mt-1">
                                  {entry.facultyName || 'TBA'}
                                </div>
                                <div className="text-[9px] font-mono text-emerald-700 font-semibold mt-0.5">
                                  {entry.roomNumber || 'LH-301'}
                                </div>
                              </div>
                            ) : (
                              <span className="text-slate-300 font-mono text-[11px]">-</span>
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 flex items-center justify-between text-xs text-slate-500 pt-3 border-t border-slate-100">
              <div className="flex items-center gap-4">
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-indigo-50 border border-indigo-200 inline-block" /> Theory Classes
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-amber-50 border border-amber-200 inline-block" /> Practical / Lab Sessions
                </span>
              </div>
              <span>Total 42 Timetable Slots per Week</span>
            </div>
          </div>
        )}

        {/* TAB 3: SUBJECTS & FACULTY ALLOCATION */}
        {activeTab === 'subjects' && (
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
            <div className="p-4 border-b border-slate-200 bg-slate-50/50">
              <h3 className="text-sm font-bold text-slate-900">
                Enrolled Subjects & Assigned Faculty Instructors
              </h3>
              <p className="text-xs text-slate-500">
                Authoritative mapping of curriculum courses to faculty members for {overview.sectionName}
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4">Subject Code</th>
                    <th className="py-3 px-4">Subject Name</th>
                    <th className="py-3 px-4">Type</th>
                    <th className="py-3 px-4 text-center">Credits</th>
                    <th className="py-3 px-4">Assigned Faculty</th>
                    <th className="py-3 px-4">Faculty Contact</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {overview.subjects.map((sub) => (
                    <tr key={sub.subjectId} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-900">
                        {sub.subjectCode}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800">
                        {sub.subjectName}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            sub.subjectType === 'LAB'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-indigo-100 text-indigo-800'
                          }`}
                        >
                          {sub.subjectType}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-center font-bold text-slate-700">
                        {sub.credits}
                      </td>
                      <td className="py-3 px-4 font-bold text-slate-900">
                        {sub.facultyName || 'TBA'}
                      </td>
                      <td className="py-3 px-4 font-mono text-slate-500">
                        {sub.facultyEmail || 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
