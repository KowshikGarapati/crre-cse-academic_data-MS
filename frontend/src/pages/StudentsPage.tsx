import React, { useEffect, useState, useMemo } from 'react';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import type { StudentDto } from '../types/auth.types';
import {
  Users,
  Search,
  Award,
  Loader2,
  CheckCircle2,
} from 'lucide-react';

export const StudentsPage: React.FC = () => {
  const [students, setStudents] = useState<StudentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    async function loadData() {
      try {
        const meta = await academicApi.getMetadata();

        // Section 1 is the prototype section
        const sec1 = meta.sections.find((s) => s.sectionName.includes('1')) || meta.sections[0];
        if (sec1) {
          const list = await academicApi.getSectionStudents(sec1.id);
          setStudents(list);
        }
      } catch (err) {
        console.error('Failed to load students', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const filteredStudents = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return students;
    return students.filter(
      (s) =>
        s.fullName.toLowerCase().includes(q) ||
        s.registrationNumber.toLowerCase().includes(q)
    );
  }, [students, search]);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Page Header */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold text-slate-900">
                    Section 1 Enrolled Students
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                    {students.length} Regular Students
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  CRRE &bull; CSE Department &bull; B.Tech Artificial Intelligence & Machine Learning &bull; 2nd Year (3rd Sem)
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <div className="p-2.5 rounded-lg bg-teal-50 border border-teal-200 text-teal-800 text-xs flex items-center gap-2">
                <Award className="w-4 h-4 text-teal-600 flex-shrink-0" />
                <span>
                  Class Teacher: <strong className="font-semibold">CH. Rama Lakshmi</strong> (DMGT)
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Filter and Search Bar */}
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-2xs mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by student name or roll number (e.g. 25B81A4201, AKHIL)..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span>Showing <strong className="text-slate-800">{filteredStudents.length}</strong> of {students.length} students</span>
            {search && (
              <button
                onClick={() => setSearch('')}
                className="text-xs text-indigo-600 hover:underline cursor-pointer"
              >
                Clear filter
              </button>
            )}
          </div>
        </div>

        {/* Students Table */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm">Loading Section 1 Students...</p>
            </div>
          ) : filteredStudents.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Users className="w-8 h-8 mx-auto text-slate-400 mb-2" />
              <p className="text-sm font-semibold text-slate-700">No students match your query</p>
              <p className="text-xs text-slate-400 mt-1">Try searching with a different roll number or name.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4 w-12 text-center">#</th>
                    <th className="py-3 px-4">Roll / Registration Number</th>
                    <th className="py-3 px-4">Student Name</th>
                    <th className="py-3 px-4">Program & Sem</th>
                    <th className="py-3 px-4">Section</th>
                    <th className="py-3 px-4">Admission Type</th>
                    <th className="py-3 px-4 text-center">Enrollment Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {filteredStudents.map((s, index) => (
                    <tr key={s.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 px-4 text-center text-slate-400 font-mono">
                        {index + 1}
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-slate-900">
                        {s.registrationNumber}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800">
                        {s.fullName}
                      </td>
                      <td className="py-3 px-4 text-slate-600">
                        B.Tech AI&ML (Sem 3)
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                          {s.sectionName || 'Section 1'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">
                          {s.entryType || 'REGULAR'}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                          <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                          {s.status || 'ENROLLED'}
                        </span>
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
