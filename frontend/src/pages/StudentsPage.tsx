import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { academicApi } from '../api/academic.api';
import { facultyApi } from '../api/faculty.api';
import { principalApi } from '../api/principal.api';
import type { MyClassDto, MySectionOverviewDto, StudentDto } from '../types/auth.types';
import {
  Users,
  Search,
  Award,
  Loader2,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

export const StudentsPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [students, setStudents] = useState<StudentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [sectionOverview, setSectionOverview] = useState<MySectionOverviewDto | null>(null);
  const [myClasses, setMyClasses] = useState<MyClassDto[]>([]);
  const [selectedClassSection, setSelectedClassSection] = useState<string>('');
  const [scopeNotice, setScopeNotice] = useState<string | null>(null);

  const isClassTeacher =
    user?.role === 'CLASS_TEACHER' ||
    user?.responsibilities?.some(
      (r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE'
    );

  useEffect(() => {
    if (!user) return;
    if (user.role === 'STUDENT') {
      navigate('/', { replace: true });
      return;
    }

    async function loadData() {
      if (!user) return;
      setLoading(true);
      setScopeNotice(null);
      try {
        if (user.role === 'PRINCIPAL') {
          const list = await principalApi.getAllStudents();
          setStudents(list);
        } else if (isClassTeacher) {
          const [list, overview] = await Promise.all([
            facultyApi.getMySectionStudents(),
            facultyApi.getMySectionOverview(),
          ]);
          setStudents(list);
          setSectionOverview(overview);
        } else if (user.role === 'FACULTY') {
          // Ordinary faculty without class teacher responsibility
          const classes = await facultyApi.getMyClasses();
          setMyClasses(classes);
          if (classes.length > 0) {
            const firstSec = classes[0].sectionId;
            setSelectedClassSection(firstSec);
            const list = await facultyApi.getStudentsForClass(firstSec);
            setStudents(list);
          } else {
            setScopeNotice('You are not currently assigned as a Class Teacher and have no taught classes.');
          }
        } else {
          // HOD or ADMIN
          const meta = await academicApi.getMetadata();
          const sec1 = meta.sections.find((s) => s.sectionName.includes('1')) || meta.sections[0];
          if (sec1) {
            const list = await academicApi.getSectionStudents(sec1.id);
            setStudents(list);
          }
        }
      } catch (err) {
        console.error('Failed to load students', err);
        setScopeNotice(err instanceof Error ? err.message : 'Access denied to this student scope.');
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [user, isClassTeacher, navigate]);

  const handleClassChange = async (sectionId: string) => {
    setSelectedClassSection(sectionId);
    setLoading(true);
    try {
      const list = await facultyApi.getStudentsForClass(sectionId);
      setStudents(list);
    } catch (err) {
      console.error('Failed to load students for class', err);
    } finally {
      setLoading(false);
    }
  };

  const filteredStudents = useMemo(() => {
    const q = search.toLowerCase().trim();
    if (!q) return students;
    return students.filter(
      (s) =>
        s.fullName.toLowerCase().includes(q) ||
        s.registrationNumber.toLowerCase().includes(q)
    );
  }, [students, search]);

  if (!user) return null;

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
                    {user.role === 'PRINCIPAL'
                      ? 'College Students Directory'
                      : isClassTeacher
                      ? `${sectionOverview?.sectionName || 'Section 1'} Enrolled Students`
                      : 'My Class Students'}
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                    {students.length} Students
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  CRRE &bull; CSE Department &bull; B.Tech Artificial Intelligence & Machine Learning &bull; 2nd Year (3rd Sem)
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {isClassTeacher && (
                <div className="p-2.5 rounded-lg bg-teal-50 border border-teal-200 text-teal-800 text-xs flex items-center gap-2">
                  <Award className="w-4 h-4 text-teal-600 flex-shrink-0" />
                  <span>
                    Class Teacher: <strong className="font-semibold">{sectionOverview?.classTeacherName || user.fullName}</strong>
                  </span>
                </div>
              )}

              {/* Class Selector for Ordinary Faculty */}
              {!isClassTeacher && user.role === 'FACULTY' && myClasses.length > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-slate-600">Select Class:</span>
                  <select
                    value={selectedClassSection}
                    onChange={(e) => handleClassChange(e.target.value)}
                    className="text-xs p-2 border border-slate-300 rounded-lg bg-white"
                  >
                    {myClasses.map((c) => (
                      <option key={c.sectionId + c.subjectId} value={c.sectionId}>
                        {c.sectionName} - {c.subjectCode} ({c.studentCount} Students)
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          </div>
        </div>

        {scopeNotice && (
          <div className="mb-6 p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-600 flex-shrink-0" />
            <span>{scopeNotice}</span>
          </div>
        )}

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
              <p className="text-sm">Loading Students...</p>
            </div>
          ) : filteredStudents.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Users className="w-8 h-8 mx-auto text-slate-400 mb-2" />
              <p className="text-sm font-semibold text-slate-700">No students found</p>
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
                        {s.programName || 'B.Tech AI&ML'} ({s.semesterName || 'Sem 3'})
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                          {s.sectionName || 'Section 1'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          s.entryType === 'LATERAL' ? 'bg-amber-100 text-amber-800' : 'bg-slate-100 text-slate-700'
                        }`}>
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
