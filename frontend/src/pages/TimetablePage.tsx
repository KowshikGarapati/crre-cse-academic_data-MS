import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import { studentApi } from '../api/student.api';
import { facultyApi } from '../api/faculty.api';
import type { FacultyTimetableEntryDto, TimetableEntryDto } from '../types/auth.types';
import {
  Calendar,
  Coffee,
  Utensils,
  Award,
  Loader2,
} from 'lucide-react';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export const TimetablePage: React.FC = () => {
  const { user } = useAuth();
  const [sectionEntries, setSectionEntries] = useState<TimetableEntryDto[]>([]);
  const [facultyEntries, setFacultyEntries] = useState<FacultyTimetableEntryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'section' | 'personal'>('section');

  useEffect(() => {
    async function loadData() {
      if (!user) return;
      setLoading(true);
      try {
        if (user.role === 'STUDENT') {
          const list = await studentApi.getMyTimetable();
          setSectionEntries(list);
          setViewMode('section');
        } else if (user.role === 'FACULTY') {
          const [facList, meta] = await Promise.all([
            facultyApi.getMyTimetable().catch(() => []),
            academicApi.getMetadata().catch(() => null),
          ]);
          setFacultyEntries(facList);
          setViewMode(facList.length > 0 ? 'personal' : 'section');

          const sec1 = meta?.sections.find((s) => s.sectionName.includes('1')) || meta?.sections[0];
          if (sec1) {
            const secList = await academicApi.getSectionTimetable(sec1.id).catch(() => []);
            setSectionEntries(secList);
          }
        } else {
          // HOD, PRINCIPAL, ADMIN
          const meta = await academicApi.getMetadata();
          const sec1 = meta.sections.find((s) => s.sectionName.includes('1')) || meta.sections[0];
          if (sec1) {
            const list = await academicApi.getSectionTimetable(sec1.id);
            setSectionEntries(list);
          }
        }
      } catch (err) {
        console.error('Failed to load timetable', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [user]);

  // Lookup helper for section timetable
  const getSectionEntry = (day: string, period: number) => {
    return sectionEntries.find(
      (e) => e.dayOfWeek?.toUpperCase() === day && e.periodNumber === period
    );
  };

  // Lookup helper for faculty personal timetable
  const getFacultyEntry = (day: string, period: number) => {
    return facultyEntries.find(
      (e) => e.dayOfWeek?.toUpperCase() === day && e.periodNumber === period
    );
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                <Calendar className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold text-slate-900">
                    {user.role === 'STUDENT'
                      ? 'My Academic Class Timetable'
                      : viewMode === 'personal'
                      ? 'My Faculty Teaching Schedule'
                      : 'Master Class Timetable • Section 1'}
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    7 Periods &bull; 09:30 AM – 04:30 PM
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  CRRE &bull; CSE Department &bull; B.Tech Artificial Intelligence & Machine Learning &bull; 2nd Year (3rd Sem)
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {user.role === 'FACULTY' && (
                <div className="flex items-center p-1 bg-slate-100 rounded-lg border border-slate-200 text-xs font-semibold">
                  <button
                    onClick={() => setViewMode('personal')}
                    className={`px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                      viewMode === 'personal'
                        ? 'bg-white text-indigo-700 shadow-2xs font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    My Teaching Slots ({facultyEntries.length})
                  </button>
                  <button
                    onClick={() => setViewMode('section')}
                    className={`px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                      viewMode === 'section'
                        ? 'bg-white text-indigo-700 shadow-2xs font-bold'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Section 1 Master
                  </button>
                </div>
              )}

              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-teal-50 text-teal-800 border border-teal-200 text-xs font-semibold">
                <Award className="w-3.5 h-3.5 text-teal-600" />
                Class Teacher: CH. Rama Lakshmi
              </span>
            </div>
          </div>
        </div>

        {/* Timetable Legend */}
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-2xs mb-6 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex flex-wrap items-center gap-4">
            <span className="font-semibold text-slate-700">Course Categories:</span>
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded bg-blue-100 border border-blue-300"></span>
              <span className="text-slate-600">Theory Courses</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded bg-amber-100 border border-amber-300"></span>
              <span className="text-slate-600">Lab Sessions</span>
            </div>
          </div>

          <div className="flex items-center gap-3 text-slate-500">
            <span className="flex items-center gap-1">
              <Coffee className="w-3.5 h-3.5 text-amber-600" /> Break: 11:10 - 11:30 AM
            </span>
            <span className="text-slate-300">|</span>
            <span className="flex items-center gap-1">
              <Utensils className="w-3.5 h-3.5 text-orange-600" /> Lunch: 01:10 - 02:10 PM
            </span>
          </div>
        </div>

        {/* Timetable Grid */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden">
          {loading ? (
            <div className="p-16 text-center text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm">Loading timetable grid...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-center border-collapse text-xs">
                <thead>
                  <tr className="bg-slate-100 text-slate-700 font-bold border-b border-slate-200">
                    <th className="p-3 border-r border-slate-200 w-28 bg-slate-200/60">Day / Period</th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 1</div>
                      <div className="text-[10px] text-slate-500 font-normal">09:30 - 10:20</div>
                    </th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 2</div>
                      <div className="text-[10px] text-slate-500 font-normal">10:20 - 11:10</div>
                    </th>
                    <th className="p-2 bg-amber-50/70 border-r border-slate-200 text-amber-900 font-medium text-[11px] w-12">
                      Break
                    </th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 3</div>
                      <div className="text-[10px] text-slate-500 font-normal">11:30 - 12:20</div>
                    </th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 4</div>
                      <div className="text-[10px] text-slate-500 font-normal">12:20 - 01:10</div>
                    </th>
                    <th className="p-2 bg-orange-50/70 border-r border-slate-200 text-orange-900 font-medium text-[11px] w-14">
                      Lunch
                    </th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 5</div>
                      <div className="text-[10px] text-slate-500 font-normal">02:10 - 03:00</div>
                    </th>
                    <th className="p-2.5 border-r border-slate-200">
                      <div>Period 6</div>
                      <div className="text-[10px] text-slate-500 font-normal">03:00 - 03:50</div>
                    </th>
                    <th className="p-2.5">
                      <div>Period 7</div>
                      <div className="text-[10px] text-slate-500 font-normal">03:50 - 04:30</div>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {DAYS.map((day) => (
                    <tr key={day} className="hover:bg-slate-50/80 transition-colors">
                      <td className="p-3 font-bold text-slate-800 bg-slate-50 border-r border-slate-200 text-left pl-4">
                        {day.slice(0, 3)}
                      </td>

                      {/* Helper to render cell */}
                      {[1, 2].map((p) => {
                        const secEntry = getSectionEntry(day, p);
                        const facEntry = getFacultyEntry(day, p);
                        const isFacMode = viewMode === 'personal';
                        const entry = isFacMode ? facEntry : secEntry;

                        return (
                          <td key={p} className="p-2 border-r border-slate-200 align-top h-20 w-32">
                            {entry ? (
                              <div className={`h-full p-2 rounded-lg border text-left flex flex-col justify-between ${
                                (isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode)?.endsWith('L')
                                  ? 'bg-amber-50/70 border-amber-200 text-amber-950'
                                  : 'bg-blue-50/70 border-blue-200 text-blue-950'
                              }`}>
                                <div>
                                  <span className="font-mono font-bold text-[11px] text-indigo-700">
                                    {isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode}
                                  </span>
                                  <div className="font-bold text-[11px] line-clamp-1">
                                    {isFacMode ? facEntry?.subjectName : secEntry?.subjectName}
                                  </div>
                                </div>
                                <div className="text-[10px] text-slate-500 mt-1 flex items-center justify-between">
                                  <span>{isFacMode ? facEntry?.sectionName : secEntry?.facultyName?.split(' ')[0]}</span>
                                  <span className="font-bold text-slate-700 font-mono">
                                    {isFacMode ? facEntry?.roomNumber || 'LH-301' : 'LH-301'}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <div className="h-full flex items-center justify-center text-slate-300 font-mono text-xs">
                                —
                              </div>
                            )}
                          </td>
                        );
                      })}

                      {/* Morning Break */}
                      <td className="p-1 bg-amber-50/40 border-r border-slate-200 text-[10px] text-amber-800 font-medium writing-mode-vertical">
                        Break
                      </td>

                      {/* Periods 3 & 4 */}
                      {[3, 4].map((p) => {
                        const secEntry = getSectionEntry(day, p);
                        const facEntry = getFacultyEntry(day, p);
                        const isFacMode = viewMode === 'personal';
                        const entry = isFacMode ? facEntry : secEntry;

                        return (
                          <td key={p} className="p-2 border-r border-slate-200 align-top h-20 w-32">
                            {entry ? (
                              <div className={`h-full p-2 rounded-lg border text-left flex flex-col justify-between ${
                                (isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode)?.endsWith('L')
                                  ? 'bg-amber-50/70 border-amber-200 text-amber-950'
                                  : 'bg-blue-50/70 border-blue-200 text-blue-950'
                              }`}>
                                <div>
                                  <span className="font-mono font-bold text-[11px] text-indigo-700">
                                    {isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode}
                                  </span>
                                  <div className="font-bold text-[11px] line-clamp-1">
                                    {isFacMode ? facEntry?.subjectName : secEntry?.subjectName}
                                  </div>
                                </div>
                                <div className="text-[10px] text-slate-500 mt-1 flex items-center justify-between">
                                  <span>{isFacMode ? facEntry?.sectionName : secEntry?.facultyName?.split(' ')[0]}</span>
                                  <span className="font-bold text-slate-700 font-mono">
                                    {isFacMode ? facEntry?.roomNumber || 'LH-301' : 'LH-301'}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <div className="h-full flex items-center justify-center text-slate-300 font-mono text-xs">
                                —
                              </div>
                            )}
                          </td>
                        );
                      })}

                      {/* Lunch */}
                      <td className="p-1 bg-orange-50/40 border-r border-slate-200 text-[10px] text-orange-800 font-medium">
                        Lunch
                      </td>

                      {/* Periods 5, 6, 7 */}
                      {[5, 6, 7].map((p) => {
                        const secEntry = getSectionEntry(day, p);
                        const facEntry = getFacultyEntry(day, p);
                        const isFacMode = viewMode === 'personal';
                        const entry = isFacMode ? facEntry : secEntry;

                        return (
                          <td key={p} className="p-2 border-r border-slate-200 align-top h-20 w-32 last:border-r-0">
                            {entry ? (
                              <div className={`h-full p-2 rounded-lg border text-left flex flex-col justify-between ${
                                (isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode)?.endsWith('L')
                                  ? 'bg-amber-50/70 border-amber-200 text-amber-950'
                                  : 'bg-blue-50/70 border-blue-200 text-blue-950'
                              }`}>
                                <div>
                                  <span className="font-mono font-bold text-[11px] text-indigo-700">
                                    {isFacMode ? facEntry?.subjectCode : secEntry?.subjectCode}
                                  </span>
                                  <div className="font-bold text-[11px] line-clamp-1">
                                    {isFacMode ? facEntry?.subjectName : secEntry?.subjectName}
                                  </div>
                                </div>
                                <div className="text-[10px] text-slate-500 mt-1 flex items-center justify-between">
                                  <span>{isFacMode ? facEntry?.sectionName : secEntry?.facultyName?.split(' ')[0]}</span>
                                  <span className="font-bold text-slate-700 font-mono">
                                    {isFacMode ? facEntry?.roomNumber || 'LH-301' : 'LH-301'}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <div className="h-full flex items-center justify-center text-slate-300 font-mono text-xs">
                                —
                              </div>
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};
