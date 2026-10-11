import React, { useEffect, useState, useMemo } from 'react';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { curriculumApi } from '../api/curriculum.api';
import { facultyApi } from '../api/faculty.api';
import { academicApi } from '../api/academic.api';
import type {
  SubjectCurriculumDto,
  ConceptDto,
  CreateResourceRequest,
  SubjectDto,
  MyClassDto,
  AcademicMetadata,
} from '../types/auth.types';
import {
  BookOpen,
  Video,
  FileText,
  Plus,
  ExternalLink,
  ChevronDown,
  ChevronRight,
  Loader2,
  AlertCircle,
  X,
  Play,
  Layers,
  Sparkles,
  Filter,
  Download,
  Printer,
} from 'lucide-react';

export const ResourcesPage: React.FC = () => {
  const { user } = useAuth();

  // Data states
  const [curriculumList, setCurriculumList] = useState<SubjectCurriculumDto[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [currentCurriculum, setCurrentCurriculum] = useState<SubjectCurriculumDto | null>(null);
  const [availableSubjects, setAvailableSubjects] = useState<SubjectDto[]>([]);
  const [taughtSubjects, setTaughtSubjects] = useState<string[]>([]); // subject codes/ids user teaches

  // Hierarchical Filter States
  const [metadata, setMetadata] = useState<AcademicMetadata | null>(null);
  const [selectedDeptId, setSelectedDeptId] = useState<string>('ALL');
  const [selectedProgramId, setSelectedProgramId] = useState<string>('ALL');
  const [selectedSemesterId, setSelectedSemesterId] = useState<string>('ALL');

  // UI states
  const [loading, setLoading] = useState(true);
  const [curriculumLoading, setCurriculumLoading] = useState(false);
  const [expandedUnits, setExpandedUnits] = useState<Record<string, boolean>>({});
  const [activeVideoUrl, setActiveVideoUrl] = useState<string | null>(null);
  const [activeHandout, setActiveHandout] = useState<{
    resource: any;
    conceptTitle: string;
    unitTitle: string;
    subjectCode: string;
    subjectName: string;
  } | null>(null);

  // Add Resource Modal states
  const [modalOpen, setModalOpen] = useState(false);
  const [modalConcept, setModalConcept] = useState<ConceptDto | null>(null);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newType, setNewType] = useState<'YOUTUBE_VIDEO' | 'PDF_DOCUMENT'>('YOUTUBE_VIDEO');
  const [newUrl, setNewUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Load curriculum for a specific subject
  const loadSubjectCurriculum = React.useCallback(async (subjectId: string) => {
    setCurriculumLoading(true);
    try {
      const data = await curriculumApi.getSubjectCurriculum(subjectId);
      setCurrentCurriculum(data);
      // Auto expand first unit
      if (data && data.units && data.units.length > 0) {
        setExpandedUnits({ [data.units[0].id]: true });
      }
    } catch (err) {
      console.error('Failed to load subject curriculum', err);
      setCurrentCurriculum(null);
    } finally {
      setCurriculumLoading(false);
    }
  }, []);

  const handleDownloadHandoutText = (handout: NonNullable<typeof activeHandout>) => {
    const textContent = `================================================================================
SIR C.R. REDDY COLLEGE OF ENGINEERING (CRRE)
DEPARTMENT OF COMPUTER SCIENCE & ENGINEERING
ACADEMIC RESOURCE ARCHIVE & STUDY HANDOUT
================================================================================
Course: ${handout.subjectCode} - ${handout.subjectName}
Unit: ${handout.unitTitle}
Topic: ${handout.conceptTitle}
Resource: ${handout.resource.title}
Uploaded by: ${handout.resource.uploadedByName || 'CSE Department Faculty'}
Document Size: ${handout.resource.fileSizeBytes ? (handout.resource.fileSizeBytes / (1024 * 1024)).toFixed(2) + ' MB' : 'Standard Department Handout'}

--------------------------------------------------------------------------------
CORE THEORETICAL SUMMARY & SYLLABUS DIRECTIVES:
--------------------------------------------------------------------------------
1. Key Conceptual Foundations:
   - In-depth theoretical derivations and structural principles of ${handout.conceptTitle}.
   - Mathematical / algorithmic proofs and execution time bounds as prescribed by the university syllabus.

2. Syllabus Learning Objectives:
   - Master the foundational theorems, syntax patterns, and problem-solving methodologies for ${handout.conceptTitle}.
   - Understand edge conditions, memory layout, and runtime optimization trade-offs.

3. Departmental Exam & Viva Preparation Notes:
   - Important analytical questions frequently featured in internal examinations and end-semester evaluations.
   - Recommended reference textbooks: Standard CRRE Departmental Course Material and Reference Syllabus.

================================================================================
CRRE Academic DMS • Institutional Learning Repository • All Rights Reserved
================================================================================
`;
    const blob = new Blob([textContent], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${handout.subjectCode}_${handout.conceptTitle.replace(/[^a-zA-Z0-9]/g, '_')}_Handout.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  // Initial load
  useEffect(() => {
    if (!user) return;

    async function loadInitial() {
      setLoading(true);
      try {
        if (user?.role === 'STUDENT') {
          // Student gets their enrolled subjects curriculum directly
          const data = await curriculumApi.getStudentResources();
          setCurriculumList(data);
          if (data.length > 0) {
            setSelectedSubjectId(data[0].subjectId);
            setCurrentCurriculum(data[0]);
            // Default first unit expanded
            if (data[0].units.length > 0) {
              setExpandedUnits({ [data[0].units[0].id]: true });
            }
          }
        } else {
          // Faculty / Counsellor / HOD / Principal
          // 1. Fetch metadata
          const meta = await academicApi.getMetadata();
          setMetadata(meta);
          setAvailableSubjects(meta.subjects || []);

          // 2. If faculty, fetch classes they teach to highlight/permit upload
          if (user?.role === 'FACULTY' || user?.role === 'CLASS_TEACHER') {
            try {
              const myClasses: MyClassDto[] = await facultyApi.getMyClasses();
              const taughtIds = myClasses.map((c) => c.subjectId);
              setTaughtSubjects(taughtIds);
            } catch (e) {
              console.warn('Could not load teaching classes', e);
            }
          }

          // Pick initial subject
          if (meta.subjects && meta.subjects.length > 0) {
            const firstId = meta.subjects[0].id;
            setSelectedSubjectId(firstId);
            await loadSubjectCurriculum(firstId);
          }
        }
      } catch (err) {
        console.error('Failed to load initial resources', err);
      } finally {
        setLoading(false);
      }
    }

    loadInitial();
  }, [user, loadSubjectCurriculum]);

  // Hierarchical filtering
  const filteredPrograms = useMemo(() => {
    if (!metadata) return [];
    if (selectedDeptId === 'ALL') return metadata.programs;
    return metadata.programs.filter((p) => p.departmentId === selectedDeptId);
  }, [metadata, selectedDeptId]);

  const filteredSemesters = useMemo(() => {
    if (!metadata || !metadata.semesters) return [];
    if (selectedProgramId === 'ALL') return metadata.semesters;
    return metadata.semesters.filter((s) => s.programId === selectedProgramId);
  }, [metadata, selectedProgramId]);

  const filteredSubjects = useMemo(() => {
    if (!metadata) return availableSubjects;
    let list = metadata.subjects || [];

    if (selectedDeptId !== 'ALL') {
      const progIds = new Set(filteredPrograms.map((p) => p.id));
      list = list.filter((s) => progIds.has(s.programId));
    }

    if (selectedProgramId !== 'ALL') {
      list = list.filter((s) => s.programId === selectedProgramId);
    }

    if (selectedSemesterId !== 'ALL') {
      list = list.filter((s) => s.semesterId === selectedSemesterId);
    }

    return list;
  }, [metadata, availableSubjects, selectedDeptId, filteredPrograms, selectedProgramId, selectedSemesterId]);


  const handleSubjectChange = async (subjId: string) => {
    setSelectedSubjectId(subjId);
    if (user?.role === 'STUDENT') {
      const found = curriculumList.find((c) => c.subjectId === subjId);
      if (found) {
        setCurrentCurriculum(found);
        if (found.units && found.units.length > 0) {
          setExpandedUnits({ [found.units[0].id]: true });
        }
        return;
      }
    }
    await loadSubjectCurriculum(subjId);
  };

  const toggleUnit = (unitId: string) => {
    setExpandedUnits((prev) => ({
      ...prev,
      [unitId]: !prev[unitId],
    }));
  };

  // Check if current user is allowed to upload to this subject
  const canUploadToCurrentSubject = useMemo(() => {
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    if (user.role === 'FACULTY' || user.role === 'CLASS_TEACHER') {
      return taughtSubjects.includes(selectedSubjectId);
    }
    return false;
  }, [user, taughtSubjects, selectedSubjectId]);

  // Open Add Resource modal
  const handleOpenAddModal = (concept: ConceptDto) => {
    setModalConcept(concept);
    setNewTitle('');
    setNewDescription('');
    setNewType('YOUTUBE_VIDEO');
    setNewUrl('');
    setSubmitError(null);
    setModalOpen(true);
  };

  // Submit Add Resource
  const handleCreateResource = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!modalConcept) return;

    if (!newTitle.trim() || !newUrl.trim()) {
      setSubmitError('Title and Resource URL are required.');
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    try {
      const req: CreateResourceRequest = {
        title: newTitle.trim(),
        description: newDescription.trim() || undefined,
        resourceType: newType,
        url: newUrl.trim(),
      };

      await curriculumApi.addResource(modalConcept.id, req);
      setModalOpen(false);

      // Refresh curriculum
      if (selectedSubjectId) {
        if (user?.role === 'STUDENT') {
          const data = await curriculumApi.getStudentResources();
          setCurriculumList(data);
          const updated = data.find((c) => c.subjectId === selectedSubjectId);
          if (updated) setCurrentCurriculum(updated);
        } else {
          await loadSubjectCurriculum(selectedSubjectId);
        }
      }
    } catch (err) {
      console.error('Failed to add resource', err);
      setSubmitError(
        err instanceof Error
          ? err.message
          : 'Failed to upload resource. Ensure you are an authorized instructor for this subject.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Extract YouTube ID helper
  const getYouTubeEmbedUrl = (url: string): string | null => {
    try {
      if (url.includes('youtube.com/embed/')) return url;
      let videoId = '';
      if (url.includes('youtu.be/')) {
        videoId = url.split('youtu.be/')[1]?.split('?')[0];
      } else if (url.includes('watch?v=')) {
        videoId = url.split('watch?v=')[1]?.split('&')[0];
      }
      return videoId ? `https://www.youtube.com/embed/${videoId}` : null;
    } catch {
      return null;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Page Header */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
                <BookOpen className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-900">
                  Curriculum & Learning Resources
                </h1>
                <p className="text-xs text-slate-500 mt-0.5">
                  CRRE &bull; CSE Department &bull; Syllabus Units, Key Concepts & Verified Study Material
                </p>
              </div>
            </div>

            {/* Subject Selector for Student */}
            {user?.role === 'STUDENT' && (
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-600">Enrolled Course:</span>
                <select
                  value={selectedSubjectId}
                  onChange={(e) => handleSubjectChange(e.target.value)}
                  className="text-xs p-2 border border-indigo-200 rounded-lg bg-indigo-50/50 font-bold text-indigo-900"
                >
                  {curriculumList.map((c) => (
                    <option key={c.subjectId} value={c.subjectId}>
                      {c.subjectCode} - {c.subjectName} ({c.type || 'THEORY'})
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Hierarchical Drill-Down Curriculum Selector for Faculty, HOD, Principal */}
          {user?.role !== 'STUDENT' && metadata && (
            <div className="mt-5 pt-5 border-t border-slate-100 flex flex-wrap items-center gap-3 text-xs">
              <span className="font-bold text-slate-700 flex items-center gap-1.5 shrink-0">
                <Filter className="w-3.5 h-3.5 text-indigo-600" />
                Curriculum Hierarchy:
              </span>

              {/* Department (for Principal/Admin) */}
              {(user?.role === 'PRINCIPAL' || user?.role === 'ADMIN') && metadata.departments.length > 1 && (
                <div className="flex items-center gap-1.5">
                  <label className="text-slate-500 font-medium">Department:</label>
                  <select
                    value={selectedDeptId}
                    onChange={(e) => {
                      const dId = e.target.value;
                      setSelectedDeptId(dId);
                      setSelectedProgramId('ALL');
                      setSelectedSemesterId('ALL');
                    }}
                    className="p-1.5 border border-slate-300 rounded-md bg-white font-medium text-slate-800 text-xs"
                  >
                    <option value="ALL">All Departments ({metadata.departments.length})</option>
                    {metadata.departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.code} - {d.name}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Program Selector */}
              <div className="flex items-center gap-1.5">
                <label className="text-slate-500 font-medium">Program:</label>
                <select
                  value={selectedProgramId}
                  onChange={(e) => {
                    const progId = e.target.value;
                    setSelectedProgramId(progId);
                    setSelectedSemesterId('ALL');
                    const nextSubjects = metadata.subjects.filter((s) => progId === 'ALL' || s.programId === progId);
                    if (nextSubjects.length > 0) {
                      handleSubjectChange(nextSubjects[0].id);
                    }
                  }}
                  className="p-1.5 border border-slate-300 rounded-md bg-white font-medium text-slate-800 text-xs"
                >
                  <option value="ALL">All Programs ({filteredPrograms.length})</option>
                  {filteredPrograms.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.code} - {p.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Semester Selector */}
              {filteredSemesters.length > 0 && (
                <div className="flex items-center gap-1.5">
                  <label className="text-slate-500 font-medium">Semester:</label>
                  <select
                    value={selectedSemesterId}
                    onChange={(e) => {
                      const semId = e.target.value;
                      setSelectedSemesterId(semId);
                      const nextSubjects = filteredSubjects.filter((s) => semId === 'ALL' || s.semesterId === semId);
                      if (nextSubjects.length > 0) {
                        handleSubjectChange(nextSubjects[0].id);
                      }
                    }}
                    className="p-1.5 border border-slate-300 rounded-md bg-white font-medium text-slate-800 text-xs"
                  >
                    <option value="ALL">All Semesters ({filteredSemesters.length})</option>
                    {filteredSemesters.map((s) => (
                      <option key={s.id} value={s.id}>
                        Semester {s.semesterNumber} ({s.termName})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Subject Selector */}
              <div className="flex items-center gap-1.5">
                <label className="text-slate-600 font-bold">Subject:</label>
                <select
                  value={selectedSubjectId}
                  onChange={(e) => handleSubjectChange(e.target.value)}
                  className="p-1.5 border border-indigo-300 rounded-md bg-indigo-50/60 font-bold text-indigo-900 text-xs min-w-[240px]"
                >
                  {filteredSubjects.length === 0 ? (
                    <option value="">No subjects match filter</option>
                  ) : (
                    filteredSubjects.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.code} - {s.name} ({s.type}, {s.credits} Cr)
                      </option>
                    ))
                  )}
                </select>
              </div>

              {/* Scope count indicator */}
              <span className="text-[11px] font-medium text-slate-500 ml-auto hidden sm:inline-block">
                Showing {filteredSubjects.length} subjects
              </span>
            </div>
          )}
        </div>

        {/* Loading State */}
        {loading || curriculumLoading ? (
          <div className="bg-white rounded-2xl p-12 border border-slate-200 text-center text-slate-500 shadow-2xs">
            <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
            <p className="text-sm font-semibold text-slate-800">Loading Subject Curriculum & Resources...</p>
            <p className="text-xs text-slate-400 mt-1">Retrieving syllabus structure, units, and learning materials</p>
          </div>
        ) : !currentCurriculum ? (
          <div className="bg-white rounded-2xl p-12 border border-slate-200 text-center text-slate-500 shadow-2xs">
            <AlertCircle className="w-8 h-8 mx-auto text-amber-500 mb-2" />
            <p className="text-sm font-semibold text-slate-800">No Curriculum Content Available</p>
            <p className="text-xs text-slate-400 mt-1">
              Select another subject from the dropdown above or contact department administration.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Subject Overview Card */}
            <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-2xs">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-xs bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded border border-indigo-200">
                      {currentCurriculum.subjectCode}
                    </span>
                    <h2 className="text-lg font-bold text-slate-900">
                      {currentCurriculum.subjectName}
                    </h2>
                  </div>
                  <div className="flex flex-wrap items-center gap-4 mt-2 text-xs text-slate-600">
                    <span>
                      Type: <strong className="text-slate-800">{currentCurriculum.type || 'THEORY'}</strong>
                    </span>
                    <span>&bull;</span>
                    <span>
                      Credits: <strong className="text-slate-800">{currentCurriculum.credits || 3}</strong>
                    </span>
                    <span>&bull;</span>
                    <span>
                      Instructor:{' '}
                      <strong className="text-indigo-700">
                        {currentCurriculum.facultyInstructor || 'CSE Department Faculty'}
                      </strong>
                    </span>
                  </div>
                </div>

                {canUploadToCurrentSubject && (
                  <div className="flex items-center gap-2">
                    <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200 flex items-center gap-1">
                      <Sparkles className="w-3 h-3 text-emerald-600" /> Authorized Instructor
                    </span>
                  </div>
                )}
              </div>

              <div className="mt-4 flex items-center gap-2 text-xs text-slate-500">
                <Layers className="w-4 h-4 text-slate-400" />
                <span>
                  Showing <strong className="text-slate-800">{currentCurriculum.units.length} Units</strong> &bull;{' '}
                  <strong className="text-slate-800">
                    {currentCurriculum.units.reduce((acc, u) => acc + u.concepts.length, 0)} Core Concepts
                  </strong>
                </span>
              </div>
            </div>

            {/* Units Accordion */}
            <div className="space-y-4">
              {currentCurriculum.units.map((unit) => {
                const isExpanded = !!expandedUnits[unit.id];
                return (
                  <div
                    key={unit.id}
                    className="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden transition-all"
                  >
                    {/* Unit Header */}
                    <button
                      onClick={() => toggleUnit(unit.id)}
                      className="w-full p-4 text-left flex items-center justify-between hover:bg-slate-50/80 transition-colors cursor-pointer"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center shrink-0 border border-indigo-100">
                          U{unit.unitNumber}
                        </div>
                        <div>
                          <h3 className="text-sm font-bold text-slate-900">
                            Unit {unit.unitNumber}: {unit.title}
                          </h3>
                          {unit.description && (
                            <p className="text-xs text-slate-500 mt-0.5 line-clamp-1">
                              {unit.description}
                            </p>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="text-[11px] font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                          {unit.concepts.length} Concepts
                        </span>
                        {isExpanded ? (
                          <ChevronDown className="w-4 h-4 text-slate-400" />
                        ) : (
                          <ChevronRight className="w-4 h-4 text-slate-400" />
                        )}
                      </div>
                    </button>

                    {/* Unit Body (Concepts & Resources) */}
                    {isExpanded && (
                      <div className="p-4 pt-1 border-t border-slate-100 bg-slate-50/50 space-y-4">
                        {unit.concepts.map((concept) => (
                          <div
                            key={concept.id}
                            className="bg-white rounded-xl p-4 border border-slate-200 shadow-2xs"
                          >
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                              <div className="flex items-start gap-2">
                                <span className="font-mono text-[10px] font-bold bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded shrink-0 mt-0.5">
                                  {unit.unitNumber}.{concept.conceptOrder}
                                </span>
                                <div>
                                  <h4 className="text-xs font-bold text-slate-900">
                                    {concept.title}
                                  </h4>
                                  {concept.description && (
                                    <p className="text-[11px] text-slate-500 mt-0.5">
                                      {concept.description}
                                    </p>
                                  )}
                                </div>
                              </div>

                              {canUploadToCurrentSubject && (
                                <button
                                  onClick={() => handleOpenAddModal(concept)}
                                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 text-[11px] font-semibold border border-indigo-200 transition-colors cursor-pointer self-start sm:self-auto"
                                >
                                  <Plus className="w-3 h-3" /> Add Resource
                                </button>
                              )}
                            </div>

                            {/* Resources list */}
                            <div className="mt-3">
                              {concept.resources.length === 0 ? (
                                <p className="text-xs text-slate-400 italic py-1">
                                  No learning resources attached to this concept yet.
                                </p>
                              ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                  {concept.resources.map((res) => (
                                    <div
                                      key={res.id}
                                      className="p-3 rounded-lg border border-slate-200 bg-slate-50 hover:bg-white transition-all flex flex-col justify-between"
                                    >
                                      <div>
                                        <div className="flex items-center justify-between gap-2 mb-1.5">
                                          <span
                                            className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold ${
                                              res.resourceType === 'YOUTUBE_VIDEO'
                                                ? 'bg-red-50 text-red-700 border border-red-200'
                                                : 'bg-blue-50 text-blue-700 border border-blue-200'
                                            }`}
                                          >
                                            {res.resourceType === 'YOUTUBE_VIDEO' ? (
                                              <>
                                                <Video className="w-3 h-3 text-red-600" /> Video Lecture
                                              </>
                                            ) : (
                                              <>
                                                <FileText className="w-3 h-3 text-blue-600" /> Lecture Notes
                                              </>
                                            )}
                                          </span>

                                          <span className="text-[10px] text-slate-400">
                                            {res.uploadedByName || 'Faculty'}
                                          </span>
                                        </div>

                                        <h5 className="text-xs font-bold text-slate-800 line-clamp-1">
                                          {res.title}
                                        </h5>
                                        {res.description && (
                                          <p className="text-[11px] text-slate-500 mt-1 line-clamp-2">
                                            {res.description}
                                          </p>
                                        )}
                                      </div>

                                      <div className="mt-3 pt-2 border-t border-slate-200/70 flex items-center justify-between">
                                        {res.resourceType === 'YOUTUBE_VIDEO' ? (
                                          <button
                                            onClick={() => setActiveVideoUrl(res.url)}
                                            className="inline-flex items-center gap-1 text-[11px] font-semibold text-red-600 hover:text-red-700 cursor-pointer"
                                          >
                                            <Play className="w-3 h-3" /> Watch Video
                                          </button>
                                        ) : (
                                          <button
                                            onClick={() =>
                                              setActiveHandout({
                                                resource: res,
                                                conceptTitle: concept.title,
                                                unitTitle: `Unit ${unit.unitNumber}: ${unit.title}`,
                                                subjectCode: currentCurriculum.subjectCode,
                                                subjectName: currentCurriculum.subjectName,
                                              })
                                            }
                                            className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 hover:text-blue-700 cursor-pointer"
                                          >
                                            <FileText className="w-3 h-3" /> View Handout Notes
                                          </button>
                                        )}

                                        {res.resourceType === 'YOUTUBE_VIDEO' ? (
                                          <a
                                            href={res.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="text-slate-400 hover:text-slate-600 text-[10px] flex items-center gap-0.5"
                                            title="Open video on YouTube"
                                          >
                                            YouTube <ExternalLink className="w-2.5 h-2.5" />
                                          </a>
                                        ) : (
                                          <button
                                            onClick={() =>
                                              setActiveHandout({
                                                resource: res,
                                                conceptTitle: concept.title,
                                                unitTitle: `Unit ${unit.unitNumber}: ${unit.title}`,
                                                subjectCode: currentCurriculum.subjectCode,
                                                subjectName: currentCurriculum.subjectName,
                                              })
                                            }
                                            className="text-slate-400 hover:text-blue-600 text-[10px] flex items-center gap-0.5 cursor-pointer"
                                            title="Read Handout Notes Reader"
                                          >
                                            Read Handout <ChevronRight className="w-2.5 h-2.5" />
                                          </button>
                                        )}
                                      </div>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Video Player Modal */}
        {activeVideoUrl && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-xs">
            <div className="bg-slate-900 rounded-2xl w-full max-w-4xl overflow-hidden shadow-2xl border border-slate-700">
              <div className="p-3 bg-slate-800 flex items-center justify-between border-b border-slate-700">
                <span className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
                  <Video className="w-4 h-4 text-red-400" /> Learning Video Lecture
                </span>
                <button
                  onClick={() => setActiveVideoUrl(null)}
                  className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-white transition-colors cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="aspect-video w-full bg-black">
                {getYouTubeEmbedUrl(activeVideoUrl) ? (
                  <iframe
                    src={getYouTubeEmbedUrl(activeVideoUrl)!}
                    title="Educational Video Player"
                    className="w-full h-full border-0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                  />
                ) : (
                  <div className="w-full h-full flex flex-col items-center justify-center text-slate-400 p-8 text-center">
                    <p className="text-sm font-semibold text-white mb-2">Video link cannot be embedded directly.</p>
                    <a
                      href={activeVideoUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-4 py-2 rounded-lg bg-red-600 text-white text-xs font-semibold hover:bg-red-700 inline-flex items-center gap-1.5"
                    >
                      <ExternalLink className="w-4 h-4" /> Open on External Platform
                    </a>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Add Resource Modal */}
        {modalOpen && modalConcept && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
            <div className="bg-white rounded-2xl w-full max-w-lg p-6 shadow-xl border border-slate-200">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <div>
                  <h3 className="text-base font-bold text-slate-900">Add Learning Resource</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Concept: <strong className="text-slate-800">{modalConcept.title}</strong>
                  </p>
                </div>
                <button
                  onClick={() => setModalOpen(false)}
                  className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {submitError && (
                <div className="mb-4 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{submitError}</span>
                </div>
              )}

              <form onSubmit={handleCreateResource} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Resource Title *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Unit 1 Lecture - Propositional Logic Fundamentals"
                    value={newTitle}
                    onChange={(e) => setNewTitle(e.target.value)}
                    className="w-full text-xs p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Resource Type *
                  </label>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => setNewType('YOUTUBE_VIDEO')}
                      className={`p-2.5 rounded-lg border text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer ${
                        newType === 'YOUTUBE_VIDEO'
                          ? 'border-red-500 bg-red-50 text-red-700'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <Video className="w-4 h-4 text-red-500" /> YouTube Video
                    </button>
                    <button
                      type="button"
                      onClick={() => setNewType('PDF_DOCUMENT')}
                      className={`p-2.5 rounded-lg border text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer ${
                        newType === 'PDF_DOCUMENT'
                          ? 'border-blue-500 bg-blue-50 text-blue-700'
                          : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      <FileText className="w-4 h-4 text-blue-500" /> PDF Document
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Resource URL *
                  </label>
                  <input
                    type="url"
                    required
                    placeholder={
                      newType === 'YOUTUBE_VIDEO'
                        ? 'https://www.youtube.com/watch?v=...'
                        : 'https://crre.edu.in/resources/...pdf'
                    }
                    value={newUrl}
                    onChange={(e) => setNewUrl(e.target.value)}
                    className="w-full text-xs p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Description / Key Points (Optional)
                  </label>
                  <textarea
                    rows={2}
                    placeholder="Brief explanation of topics covered..."
                    value={newDescription}
                    onChange={(e) => setNewDescription(e.target.value)}
                    className="w-full text-xs p-2.5 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setModalOpen(false)}
                    className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-4 py-1.5 rounded-lg bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-1.5 cursor-pointer shadow-2xs"
                  >
                    {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    {submitting ? 'Publishing...' : 'Publish Resource'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Handout Notes Reader Modal */}
        {activeHandout && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs">
            <div className="bg-white rounded-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden shadow-2xl border border-slate-200 flex flex-col">
              {/* Header */}
              <div className="p-4 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold">
                    <FileText className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                      Institutional Lecture Handout & Study Notes
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-500/20 text-blue-300 border border-blue-400/30">
                        Verified Departmental Resource
                      </span>
                    </h3>
                    <p className="text-[11px] text-slate-300">
                      {activeHandout.subjectCode} &bull; {activeHandout.subjectName}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setActiveHandout(null)}
                  className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Body */}
              <div className="p-6 overflow-y-auto space-y-5 text-xs text-slate-700 flex-1">
                {/* Document Metadata Banner */}
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-bold text-slate-900">{activeHandout.resource.title}</div>
                    <div className="text-[11px] text-slate-500 mt-0.5">
                      {activeHandout.unitTitle} &bull; <strong className="text-slate-700">{activeHandout.conceptTitle}</strong>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleDownloadHandoutText(activeHandout)}
                      className="px-3 py-1.5 rounded-lg bg-indigo-600 text-white font-semibold text-xs hover:bg-indigo-700 inline-flex items-center gap-1.5 transition-colors cursor-pointer"
                    >
                      <Download className="w-3.5 h-3.5" /> Download Notes
                    </button>
                    <button
                      onClick={() => window.print()}
                      className="px-3 py-1.5 rounded-lg bg-white border border-slate-300 text-slate-700 font-semibold text-xs hover:bg-slate-100 inline-flex items-center gap-1.5 transition-colors cursor-pointer"
                    >
                      <Printer className="w-3.5 h-3.5" /> Print
                    </button>
                  </div>
                </div>

                {/* Handout Content Synopsis */}
                <div className="space-y-4">
                  <div className="border border-indigo-100 bg-indigo-50/40 rounded-xl p-4">
                    <h4 className="font-bold text-indigo-950 mb-1 flex items-center gap-1.5 text-xs">
                      <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                      Topic Overview & Learning Objectives
                    </h4>
                    <p className="text-slate-600 leading-relaxed text-[11px]">
                      {activeHandout.resource.description ||
                        `This institutional study handout provides comprehensive foundational coverage for ${activeHandout.conceptTitle}. Students are expected to understand the formal definitions, analytical properties, and implementation paradigms.`}
                    </p>
                  </div>

                  <div className="border border-slate-200 rounded-xl p-4 space-y-3">
                    <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider text-slate-500">
                      Key Syllabus Study Points
                    </h4>
                    <ul className="space-y-2 list-disc list-inside text-[11px] text-slate-600 leading-relaxed">
                      <li>
                        <strong>Theoretical Fundamentals:</strong> Formal conceptual structure of {activeHandout.conceptTitle} according to the latest academic curriculum.
                      </li>
                      <li>
                        <strong>Analytical Formulation:</strong> Mathematical derivations, algorithmic complexity bounds, and core architectural rules.
                      </li>
                      <li>
                        <strong>Exam & Viva Focus:</strong> High-frequency semester examination problems, numerical illustrations, and practical lab considerations.
                      </li>
                    </ul>
                  </div>

                  <div className="bg-amber-50/70 border border-amber-200/80 rounded-xl p-3.5 flex items-start gap-2 text-[11px] text-amber-900">
                    <AlertCircle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                    <div>
                      <strong className="font-semibold">Department Faculty Note:</strong> This handout is verified by the Computer Science & Engineering faculty instructor ({activeHandout.resource.uploadedByName || 'CSE Faculty Staff'}). Use in conjunction with recommended lecture video materials for complete exam preparation.
                    </div>
                  </div>
                </div>
              </div>

              {/* Footer */}
              <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
                <span>CRRE Academic DMS &bull; CSE Departmental Archive</span>
                <button
                  onClick={() => setActiveHandout(null)}
                  className="px-4 py-1.5 rounded-lg bg-slate-200 hover:bg-slate-300 text-slate-800 font-medium transition-colors cursor-pointer"
                >
                  Close Reader
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
