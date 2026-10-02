import React, { useEffect, useState } from 'react';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import type { TimetableEntryDto } from '../types/auth.types';
import {
  Calendar,
  Coffee,
  Utensils,
  Award,
  Loader2,
} from 'lucide-react';

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export const TimetablePage: React.FC = () => {
  const [entries, setEntries] = useState<TimetableEntryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const meta = await academicApi.getMetadata();
        const sec1 = meta.sections.find((s) => s.sectionName.includes('1')) || meta.sections[0];
        if (sec1) {
          const list = await academicApi.getSectionTimetable(sec1.id);
          setEntries(list);
        }
      } catch (err) {
        console.error('Failed to load timetable', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  // Lookup helper: day + period -> entry
  const getEntry = (day: string, period: number) => {
    return entries.find(
      (e) => e.dayOfWeek?.toUpperCase() === day && e.periodNumber === period
    );
  };

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
                    Master Class Timetable &bull; Section 1
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

            <div className="flex items-center gap-2 text-xs">
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-teal-50 text-teal-800 border border-teal-200">
                <Award className="w-3.5 h-3.5 text-teal-600" />
                Class Teacher: CH. Rama Lakshmi (DMGT)
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
              <span className="text-slate-600">Theory Courses (3.0 Credits)</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-3 h-3 rounded bg-amber-100 border border-amber-300"></span>
              <span className="text-slate-600">Lab Sessions (1.5 Credits)</span>
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
            <div className="p-12 text-center text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm">Loading weekly schedule...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-center text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200">
                    <th className="py-3 px-3 w-28 text-left border-r border-slate-200">Day</th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 1</div>
                      <div className="text-[10px] text-slate-400 font-normal">09:30 - 10:20</div>
                    </th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 2</div>
                      <div className="text-[10px] text-slate-400 font-normal">10:20 - 11:10</div>
                    </th>
                    <th className="py-2 px-1 bg-amber-50/70 text-amber-800 border-r border-slate-200 w-12">
                      <div className="text-[10px] font-bold">BREAK</div>
                      <div className="text-[9px] text-amber-600 font-normal">20m</div>
                    </th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 3</div>
                      <div className="text-[10px] text-slate-400 font-normal">11:30 - 12:20</div>
                    </th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 4</div>
                      <div className="text-[10px] text-slate-400 font-normal">12:20 - 01:10</div>
                    </th>
                    <th className="py-2 px-1 bg-orange-50/70 text-orange-800 border-r border-slate-200 w-14">
                      <div className="text-[10px] font-bold">LUNCH</div>
                      <div className="text-[9px] text-orange-600 font-normal">1 Hour</div>
                    </th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 5</div>
                      <div className="text-[10px] text-slate-400 font-normal">02:10 - 03:00</div>
                    </th>
                    <th className="py-2 px-2 border-r border-slate-200">
                      <div>Period 6</div>
                      <div className="text-[10px] text-slate-400 font-normal">03:00 - 03:50</div>
                    </th>
                    <th className="py-2 px-2">
                      <div>Period 7</div>
                      <div className="text-[10px] text-slate-400 font-normal">03:50 - 04:30</div>
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {DAYS.map((day) => (
                    <tr key={day} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3 px-3 text-left font-bold text-slate-800 bg-slate-50 border-r border-slate-200 text-xs">
                        {day}
                      </td>

                      {/* Period 1 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 1))}
                      </td>

                      {/* Period 2 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 2))}
                      </td>

                      {/* Break Slot */}
                      <td className="p-1 bg-amber-50/40 border-r border-slate-200 text-amber-800 text-[10px] font-medium vertical-middle">
                        <div className="rotate-180 writing-mode-vertical py-2 mx-auto">
                          Break
                        </div>
                      </td>

                      {/* Period 3 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 3))}
                      </td>

                      {/* Period 4 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 4))}
                      </td>

                      {/* Lunch Slot */}
                      <td className="p-1 bg-orange-50/40 border-r border-slate-200 text-orange-800 text-[10px] font-medium vertical-middle">
                        <div className="rotate-180 writing-mode-vertical py-2 mx-auto">
                          Lunch
                        </div>
                      </td>

                      {/* Period 5 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 5))}
                      </td>

                      {/* Period 6 */}
                      <td className="p-1.5 border-r border-slate-200 align-top">
                        {renderSlot(getEntry(day, 6))}
                      </td>

                      {/* Period 7 */}
                      <td className="p-1.5 align-top">
                        {renderSlot(getEntry(day, 7))}
                      </td>
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

function renderSlot(entry?: TimetableEntryDto) {
  if (!entry) {
    return (
      <div className="h-full min-h-[56px] rounded bg-slate-50/50 border border-dashed border-slate-200 flex items-center justify-center text-slate-300 text-[11px]">
        Free
      </div>
    );
  }

  const isLab = entry.subjectName?.toLowerCase().includes('lab');

  return (
    <div
      className={`h-full min-h-[56px] p-2 rounded-lg text-left transition-all border ${
        isLab
          ? 'bg-amber-50/80 border-amber-200 hover:border-amber-400'
          : 'bg-indigo-50/70 border-indigo-200 hover:border-indigo-400'
      }`}
    >
      <div className="flex items-center justify-between gap-1">
        <span className="font-bold text-slate-900 text-[11px] leading-tight line-clamp-1">
          {entry.subjectName}
        </span>
        <span
          className={`text-[9px] px-1 py-0.2 rounded font-mono font-semibold ${
            isLab ? 'bg-amber-200 text-amber-800' : 'bg-indigo-200 text-indigo-800'
          }`}
        >
          {entry.subjectCode}
        </span>
      </div>
      <div className="text-[10px] text-slate-600 mt-1 flex items-center justify-between">
        <span className="truncate">{entry.facultyName || 'Staff'}</span>
      </div>
    </div>
  );
}
