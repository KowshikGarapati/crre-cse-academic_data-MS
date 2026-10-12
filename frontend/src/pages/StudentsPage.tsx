import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { studentApi } from '../api/student.api';
import { facultyApi } from '../api/faculty.api';
import { academicApi } from '../api/academic.api';
import type { AcademicMetadata, MyClassDto, MySectionOverviewDto, StudentDto } from '../types/auth.types';
import { isUserHod } from '../types/auth.types';
import {
  Users,
  Search,
  Loader2,
  CheckCircle2,
  AlertTriangle,
  ChevronRight,
  Filter,
  Edit,
} from 'lucide-react';
import { StudentEditModal } from '../components/StudentEditModal';

export const StudentsPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [students, setStudents] = useState<StudentDto[]>([]);
  const [selectedStudentForEdit, setSelectedStudentForEdit] = useState<StudentDto | null>(null);
  const [metadata, setMetadata] = useState<AcademicMetadata | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [sectionOverview, setSectionOverview] = useState<MySectionOverviewDto | null>(null);
  const [myClasses, setMyClasses] = useState<MyClassDto[]>([]);
  const [selectedClassSection, setSelectedClassSection] = useState<string>('MY_SECTION');
  const [scopeNotice, setScopeNotice] = useState<string | null>(null);

  // Hierarchical Filter States for HOD / Principal
  const [selectedDeptId, setSelectedDeptId] = useState<string>('ALL');
  const [selectedProgramId, setSelectedProgramId] = useState<string>('ALL');
  const [selectedSectionId, setSelectedSectionId] = useState<string>('ALL');

  const isClassTeacher =
    user?.role === 'CLASS_TEACHER' ||
    user?.responsibilities?.some(
      (r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE'
    );

  const isHod = isUserHod(user);

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
        // Load metadata for hierarchical filtering
        const meta = await academicApi.getMetadata().catch(() => null);
        setMetadata(meta);

        if (isClassTeacher) {
          // Class Teacher Scope: Load ONLY the corresponding section's students
          const [overview, sectionRoster, classes] = await Promise.all([
            facultyApi.getMySectionOverview().catch(() => null),
            facultyApi.getMySectionStudents().catch(() => []),
            facultyApi.getMyClasses().catch(() => []),
          ]);
          setSectionOverview(overview);
          setStudents(sectionRoster);
          setMyClasses(classes);
          setSelectedClassSection('MY_SECTION');
        } else if (user.role === 'FACULTY' && !isHod) {
          // Teaching Faculty Scope
          const [classes, list] = await Promise.all([
            facultyApi.getMyClasses().catch(() => []),
            studentApi.getScopedStudents(),
          ]);
          setMyClasses(classes);
          setStudents(list);
          setSelectedClassSection('ALL');
        } else {
          // HOD, PRINCIPAL, ADMIN
          const list = await studentApi.getScopedStudents();
          setStudents(list);
        }
      } catch (err) {
        console.error('Failed to load students', err);
        setScopeNotice(err instanceof Error ? err.message : 'Access denied to this student scope.');
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [user, isClassTeacher, isHod, navigate]);

  // Handle Class Teacher / Faculty section switch
  const handleClassChange = async (sectionChoice: string) => {
    setSelectedClassSection(sectionChoice);
    setLoading(true);
    try {
      if (sectionChoice === 'MY_SECTION') {
        const list = await facultyApi.getMySectionStudents();
        setStudents(list);
      } else if (sectionChoice === 'ALL') {
        const list = await studentApi.getScopedStudents();
        setStudents(list);
      } else {
        const list = await facultyApi.getStudentsForClass(sectionChoice);
        setStudents(list);
      }
    } catch (err) {
      console.error('Failed to load students for class', err);
    } finally {
      setLoading(false);
    }
  };

  // Cascading programs for Principal
  const availablePrograms = useMemo(() => {
    if (!metadata) return [];
    if (selectedDeptId === 'ALL') return metadata.programs;
    return metadata.programs.filter((p) => p.departmentId === selectedDeptId);
  }, [metadata, selectedDeptId]);

  // Cascading sections for HOD / Principal
  const availableSections = useMemo(() => {
    if (!metadata) return [];
    if (selectedProgramId === 'ALL') {
      if (selectedDeptId === 'ALL') return metadata.sections;
      const validProgIds = new Set(availablePrograms.map((p) => p.id));
      const validSemIds = new Set(
        metadata.subjects.filter((s) => validProgIds.has(s.programId)).map((s) => s.semesterId)
      );
      return metadata.sections.filter((sec) => {
        if (sec.programCode) {
          return availablePrograms.some((p) => p.code === sec.programCode);
        }
        return validSemIds.has(sec.semesterId);
      });
    }

    const prog = metadata.programs.find((p) => p.id === selectedProgramId);
    const progSubjects = metadata.subjects.filter((s) => s.programId === selectedProgramId);
    const validSemesterIds = new Set(progSubjects.map((s) => s.semesterId));
    return metadata.sections.filter((sec) => {
      if (sec.programCode && prog) {
        return sec.programCode === prog.code;
      }
      return validSemesterIds.has(sec.semesterId);
    });
  }, [metadata, selectedDeptId, selectedProgramId, availablePrograms]);

  // Combined hierarchical and text search filtering
  const filteredStudents = useMemo(() => {
    let result = students;

    // Filter strictly by hierarchical section ID if selected
    if (selectedSectionId !== 'ALL') {
      result = result.filter((s) => s.sectionId === selectedSectionId);
    } else if (selectedProgramId !== 'ALL') {
      const prog = metadata?.programs.find((p) => p.id === selectedProgramId);
      if (prog) {
        result = result.filter((s) =>
          s.programName?.toLowerCase().includes(prog.code.toLowerCase()) ||
          s.programName?.toLowerCase().includes(prog.name.toLowerCase())
        );
      }
    }

    // Text search
    const q = search.toLowerCase().trim();
    if (q) {
      result = result.filter(
        (s) =>
          s.fullName.toLowerCase().includes(q) ||
          s.registrationNumber.toLowerCase().includes(q)
      );
    }

    return result;
  }, [students, selectedSectionId, selectedProgramId, metadata, search]);

  if (!user || user.role === 'STUDENT') return null;

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
                      : (user.role === 'HOD' || isHod)
                      ? 'CSE Department Students Roster'
                      : user.role === 'COUNSELLOR'
                      ? 'Assigned Mentee Students'
                      : isClassTeacher
                      ? `${sectionOverview?.sectionName || 'My Section'} Enrolled Students`
                      : 'My Teaching Students'}
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                    {filteredStudents.length} Students
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  {user.role === 'PRINCIPAL'
                    ? 'CRRE Engineering • Institution-wide Student Scope & Hierarchical Navigation'
                    : (user.role === 'HOD' || isHod)
                    ? 'CSE Department • 4 Programs • 10 Sections • Hierarchical Student Explorer'
                    : isClassTeacher
                    ? `CSE Department • Dedicated Class Teacher Scope (${sectionOverview?.sectionName || 'Assigned Section'}) • Showing Only Corresponding Section Students`
                    : 'CSE Department • Faculty Subject Teaching Scope'}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {/* Class Teacher Section Controller */}
              {isClassTeacher && (
                <div className="flex items-center gap-2 p-1.5 bg-slate-100 rounded-lg border border-slate-200 text-xs">
                  <span className="font-semibold text-slate-600 px-1">Scope:</span>
                  <select
                    value={selectedClassSection}
                    onChange={(e) => handleClassChange(e.target.value)}
                    className="p-1.5 border border-teal-300 rounded-md bg-white font-bold text-teal-900 text-xs shadow-2xs"
                  >
                    <option value="MY_SECTION">
                      ★ My Section: {sectionOverview?.sectionName || 'Assigned'} ({sectionOverview?.totalStudents || students.length} Students)
                    </option>
                    {myClasses.map((c) => (
                      <option key={c.sectionId + c.subjectId} value={c.sectionId}>
                        Teaching: {c.sectionName} - {c.subjectCode} ({c.studentCount} Students)
                      </option>
                    ))}
                    <option value="ALL">All Teaching Sections Combined</option>
                  </select>
                </div>
              )}

              {/* Ordinary Faculty Class Selector */}
              {!isClassTeacher && user.role === 'FACULTY' && !isHod && myClasses.length > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-slate-600">Select Class:</span>
                  <select
                    value={selectedClassSection}
                    onChange={(e) => handleClassChange(e.target.value)}
                    className="text-xs p-2 border border-slate-300 rounded-lg bg-white font-medium"
                  >
                    <option value="ALL">All My Teaching Sections ({students.length})</option>
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

          {/* Hierarchical Selector Bar for Principal and HOD */}
          {(user.role === 'PRINCIPAL' || user.role === 'ADMIN' || isHod || user.role === 'HOD') && metadata && (
            <div className="mt-5 pt-5 border-t border-slate-100 flex flex-wrap items-center gap-3 text-xs">
              <span className="font-bold text-slate-700 flex items-center gap-1.5">
                <Filter className="w-3.5 h-3.5 text-indigo-600" />
                Hierarchical Selector:
              </span>

              {/* Department filter (Principal only) */}
              {(user.role === 'PRINCIPAL' || user.role === 'ADMIN') && (
                <div className="flex items-center gap-1.5">
                  <label className="text-slate-500 font-medium">Department:</label>
                  <select
                    value={selectedDeptId}
                    onChange={(e) => {
                      setSelectedDeptId(e.target.value);
                      setSelectedProgramId('ALL');
                      setSelectedSectionId('ALL');
                    }}
                    className="p-1.5 border border-slate-300 rounded-md bg-white font-medium text-slate-800 text-xs"
                  >
                    <option value="ALL">All Departments</option>
                    {metadata.departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.code} - {d.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Program filter */}
              <div className="flex items-center gap-1.5">
                <label className="text-slate-500 font-medium">Program:</label>
                <select
                  value={selectedProgramId}
                  onChange={(e) => {
                    setSelectedProgramId(e.target.value);
                    setSelectedSectionId('ALL');
                  }}
                  className="p-1.5 border border-slate-300 rounded-md bg-white font-medium text-slate-800 text-xs"
                >
                  <option value="ALL">All Programs</option>
                  {availablePrograms.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.code} - {p.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Section filter */}
              <div className="flex items-center gap-1.5">
                <label className="text-slate-500 font-medium">Section:</label>
                <select
                  value={selectedSectionId}
                  onChange={(e) => setSelectedSectionId(e.target.value)}
                  className="p-1.5 border border-indigo-300 rounded-md bg-indigo-50/50 font-bold text-indigo-900 text-xs"
                >
                  <option value="ALL">All Sections ({availableSections.length})</option>
                  {availableSections.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.programCode ? `${s.programCode} - ` : ''}{s.sectionName}
                    </option>
                  ))}
                </select>
              </div>

              {(selectedProgramId !== 'ALL' || selectedSectionId !== 'ALL' || selectedDeptId !== 'ALL') && (
                <button
                  onClick={() => {
                    setSelectedDeptId('ALL');
                    setSelectedProgramId('ALL');
                    setSelectedSectionId('ALL');
                  }}
                  className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold underline cursor-pointer ml-auto"
                >
                  Reset Hierarchy Filters
                </button>
              )}
            </div>
          )}
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
            <span>
              Showing <strong className="text-slate-800">{filteredStudents.length}</strong> of {students.length} students
            </span>
            {search && (
              <button
                onClick={() => setSearch('')}
                className="text-xs text-indigo-600 hover:underline cursor-pointer"
              >
                Clear text filter
              </button>
            )}
          </div>
        </div>

        {/* Students Table */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm font-semibold text-slate-700">Loading Students Directory...</p>
              <p className="text-xs text-slate-400 mt-1">Applying institutional scope and section enrollments</p>
            </div>
          ) : filteredStudents.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Users className="w-8 h-8 mx-auto text-slate-400 mb-2" />
              <p className="text-sm font-semibold text-slate-700">No students found matching current criteria</p>
              <p className="text-xs text-slate-400 mt-1">Try resetting the hierarchical filters or clearing your search query.</p>
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
                    <th className="py-3 px-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {filteredStudents.map((s, index) => (
                    <tr
                      key={s.id}
                      onClick={() => navigate(`/students/${s.registrationNumber || s.id}`)}
                      className="hover:bg-indigo-50/70 transition-colors cursor-pointer group"
                      title="Click to view academic details"
                    >
                      <td className="py-3 px-4 text-center text-slate-400 font-mono">
                        {index + 1}
                      </td>
                      <td className="py-3 px-4 font-mono font-bold text-slate-900 group-hover:text-indigo-700">
                        {s.registrationNumber}
                      </td>
                      <td className="py-3 px-4 font-semibold text-slate-800 group-hover:text-indigo-900">
                        {s.fullName}
                      </td>
                      <td className="py-3 px-4 text-slate-600">
                        {s.programName || 'Degree Program'} ({s.semesterName || 'Sem'})
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
                          {s.sectionName || 'Section'}
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
                      <td className="py-3 px-3 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {user?.role !== 'STUDENT' && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                setSelectedStudentForEdit(s);
                              }}
                              className="px-2 py-1 text-[11px] font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded border border-indigo-200 transition-colors cursor-pointer flex items-center gap-1"
                              title="Edit Student"
                            >
                              <Edit className="w-3 h-3" /> Edit
                            </button>
                          )}
                          <span className="inline-flex items-center text-slate-400 group-hover:text-indigo-600 font-semibold text-[11px] gap-0.5">
                            View <ChevronRight className="w-3.5 h-3.5" />
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {selectedStudentForEdit && (
          <StudentEditModal
            isOpen={Boolean(selectedStudentForEdit)}
            onClose={() => setSelectedStudentForEdit(null)}
            studentId={selectedStudentForEdit.id}
            metadata={metadata}
            onSaved={(updated) => {
              setStudents((prev) =>
                prev.map((item) =>
                  item.id === updated.id || item.registrationNumber === updated.registrationNumber
                    ? {
                        ...item,
                        fullName: updated.fullName,
                        entryType: updated.entryType,
                        status: updated.status,
                        sectionId: updated.placement?.sectionId || item.sectionId,
                        sectionName: updated.placement?.sectionName || item.sectionName,
                      }
                    : item
                )
              );
            }}
          />
        )}
      </main>
    </div>
  );
};
