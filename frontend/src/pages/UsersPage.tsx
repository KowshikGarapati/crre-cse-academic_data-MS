import React, { useEffect, useState, useMemo } from 'react';
import { Navbar } from '../components/Navbar';
import { useAuth } from '../context/useAuth';
import { userApi } from '../api/user.api';
import { responsibilityApi } from '../api/responsibility.api';
import { academicApi } from '../api/academic.api';
import type {
  UserSummary,
  Role,
  CreateUserRequest,
  ResponsibilityRequest,
  ResponsibilitySummary,
  AcademicMetadata,
} from '../types/auth.types';
import {
  Users,
  UserPlus,
  Search,
  CheckCircle,
  XCircle,
  Plus,
  Trash2,
  AlertTriangle,
  Loader2,
  X,
  Briefcase,
  Eye,
  EyeOff,
} from 'lucide-react';

export const UsersPage: React.FC = () => {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [metadata, setMetadata] = useState<AcademicMetadata | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedRoleFilter, setSelectedRoleFilter] = useState<string>('ALL');

  // Add User Modal State
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [activeAddTab, setActiveAddTab] = useState<'STUDENT' | 'FACULTY' | 'OTHER'>('STUDENT');
  const [showStudentPassword, setShowStudentPassword] = useState(false);
  const [showFacultyPassword, setShowFacultyPassword] = useState(false);
  const [showOtherPassword, setShowOtherPassword] = useState(false);

  // Student specific form with Educational History
  const [studentForm, setStudentForm] = useState({
    fullName: '',
    username: '',
    email: '',
    password: 'Student@123',
    registrationNumber: '',
    uniqueId: '',
    entryType: 'REGULAR',
    educationalHistory: [
      {
        qualificationType: 'SSC',
        boardOrUniversity: 'BSEAP',
        institutionName: 'Govt High School',
        hallTicketNumber: '',
        completionYear: 2022,
        percentage: 85,
      },
    ],
  });

  // Faculty specific form with Professional details (NO PROGRAM FIELD)
  const [facultyForm, setFacultyForm] = useState({
    fullName: '',
    username: '',
    email: '',
    password: 'Faculty@123',
    employeeNumber: '',
    designation: 'Assistant Professor',
    qualification: 'M.Tech (CSE)',
    specialization: 'Artificial Intelligence & Machine Learning',
    professionalExperience: '5+ Years',
    institutionalContact: 'ext: 4201',
    subjectId: '',
    sectionId: '',
    classTeacher: false,
    counsellor: false,
  });

  const [addForm, setAddForm] = useState<Partial<CreateUserRequest>>({
    role: 'HOD',
    classTeacher: false,
  });
  const [addError, setAddError] = useState<string | null>(null);
  const [isSubmittingUser, setIsSubmittingUser] = useState(false);

  // Manage Responsibilities Modal State
  const [selectedUserForResp, setSelectedUserForResp] = useState<UserSummary | null>(null);
  const [userResponsibilities, setUserResponsibilities] = useState<ResponsibilitySummary[]>([]);
  const [loadingResp, setLoadingResp] = useState(false);
  const [respForm, setRespForm] = useState<Partial<ResponsibilityRequest>>({
    type: 'TEACHING',
  });
  const [respError, setRespError] = useState<string | null>(null);
  const [isSubmittingResp, setIsSubmittingResp] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  // Downward authority roles for creation
  const allowedRolesToCreate: Role[] = useMemo(() => {
    if (currentUser?.role === 'ADMIN' || currentUser?.role === 'PRINCIPAL') {
      return ['PRINCIPAL', 'HOD', 'FACULTY', 'COUNSELLOR', 'CLASS_TEACHER', 'STUDENT'];
    }
    if (currentUser?.role === 'HOD') {
      // HOD cannot create PRINCIPAL or ADMIN
      return ['FACULTY', 'COUNSELLOR', 'CLASS_TEACHER', 'STUDENT'];
    }
    return [];
  }, [currentUser]);

  useEffect(() => {
    let mounted = true;
    async function loadData() {
      try {
        const [usersList, meta] = await Promise.all([
          userApi.getAllUsers(),
          academicApi.getMetadata(),
        ]);
        if (mounted) {
          setUsers(usersList);
          setMetadata(meta);
          if (meta.subjects.length > 0) {
            setFacultyForm((prev) => ({ ...prev, subjectId: meta.subjects[0].id }));
          }
          if (meta.sections.length > 0) {
            setFacultyForm((prev) => ({ ...prev, sectionId: meta.sections[0].id }));
          }
        }
      } catch (err) {
        console.error('Failed to load user management data', err);
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }
    loadData();
    return () => {
      mounted = false;
    };
  }, []);

  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchesRole =
        selectedRoleFilter === 'ALL' || u.role === selectedRoleFilter;
      const q = search.toLowerCase().trim();
      const matchesSearch =
        !q ||
        u.fullName.toLowerCase().includes(q) ||
        u.username.toLowerCase().includes(q) ||
        (u.registrationNumber && u.registrationNumber.toLowerCase().includes(q)) ||
        (u.uniqueId && u.uniqueId.toLowerCase().includes(q));
      return matchesRole && matchesSearch;
    });
  }, [users, selectedRoleFilter, search]);

  const handleToggleStatus = async (userToToggle: UserSummary) => {
    try {
      await userApi.toggleUserStatus(userToToggle.id, !userToToggle.enabled);
      setUsers((prev) =>
        prev.map((u) =>
          u.id === userToToggle.id ? { ...u, enabled: !u.enabled } : u
        )
      );
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to update user status');
    }
  };

  const handleCreateStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    const fullName = studentForm.fullName.trim();
    const username = studentForm.username.trim();
    const email = studentForm.email.trim();
    const password = studentForm.password.trim();
    const regNo = studentForm.registrationNumber.trim();
    const uniqueId = studentForm.uniqueId.trim() || ('CRRE' + regNo);

    if (!fullName || !username || !email || !password || !regNo) {
      setAddError('Please fill in all required student details and provide a password');
      return;
    }
    const cseDept = metadata?.departments.find((d) => d.code === 'CSE') || metadata?.departments[0];
    const aimlProg = metadata?.programs.find((p) => p.code === 'AIML') || metadata?.programs[0];
    const sec1 = metadata?.sections.find((s) => s.sectionName.includes('1')) || metadata?.sections[0];

    setAddError(null);
    setIsSubmittingUser(true);
    try {
      const created = await userApi.createStudentUser({
        fullName,
        username,
        email,
        password,
        registrationNumber: regNo,
        uniqueId,
        entryType: studentForm.entryType,
        departmentId: cseDept!.id,
        programId: aimlProg!.id,
        academicYearId: '40000000-0000-0000-0000-000000000001',
        semesterId: '50000000-0000-0000-0000-000000000001',
        sectionId: sec1!.id,
        educationalHistory: studentForm.educationalHistory.map((h) => ({
          qualificationType: h.qualificationType,
          boardOrUniversity: h.boardOrUniversity.trim(),
          institutionName: h.institutionName.trim(),
          hallTicketNumber: h.hallTicketNumber?.trim() || undefined,
          completionYear: Number(h.completionYear),
          percentage: Number(h.percentage),
        })),
      });
      setUsers((prev) => [created, ...prev]);
      setShowAddUserModal(false);
      setStudentForm({
        fullName: '',
        username: '',
        email: '',
        password: 'Student@123',
        registrationNumber: '',
        uniqueId: '',
        entryType: 'REGULAR',
        educationalHistory: [
          {
            qualificationType: 'SSC',
            boardOrUniversity: 'BSEAP',
            institutionName: 'Govt High School',
            hallTicketNumber: '',
            completionYear: 2022,
            percentage: 85,
          },
        ],
      });
    } catch (err: unknown) {
      setAddError(err instanceof Error ? err.message : 'Failed to create student');
    } finally {
      setIsSubmittingUser(false);
    }
  };

  const handleCreateFaculty = async (e: React.FormEvent) => {
    e.preventDefault();
    const fullName = facultyForm.fullName.trim();
    const username = facultyForm.username.trim();
    const email = facultyForm.email.trim();
    const password = facultyForm.password.trim();
    const employeeNumber = facultyForm.employeeNumber.trim();

    if (!fullName || !username || !email || !password || !employeeNumber) {
      setAddError('Please fill in all required faculty details and provide a password');
      return;
    }
    const cseDept = metadata?.departments.find((d) => d.code === 'CSE') || metadata?.departments[0];
    const sec1 = metadata?.sections.find((s) => s.sectionName.includes('1')) || metadata?.sections[0];
    const sub1 = metadata?.subjects[0];

    const teachingAssignments = facultyForm.subjectId && facultyForm.sectionId
      ? [{ subjectId: facultyForm.subjectId, sectionId: facultyForm.sectionId }]
      : sub1 && sec1 ? [{ subjectId: sub1.id, sectionId: sec1.id }] : [];

    setAddError(null);
    setIsSubmittingUser(true);
    try {
      const created = await userApi.createFacultyUser({
        fullName,
        username,
        email,
        password,
        employeeNumber,
        departmentId: cseDept!.id,
        designation: facultyForm.designation.trim(),
        qualification: facultyForm.qualification?.trim(),
        specialization: facultyForm.specialization?.trim(),
        professionalExperience: facultyForm.professionalExperience?.trim(),
        institutionalContact: facultyForm.institutionalContact?.trim(),
        teachingAssignments,
        classTeacherSectionId: facultyForm.classTeacher ? (facultyForm.sectionId || sec1?.id) : undefined,
        counsellor: facultyForm.counsellor,
      });
      setUsers((prev) => [created, ...prev]);
      setShowAddUserModal(false);
      setFacultyForm({
        fullName: '',
        username: '',
        email: '',
        password: 'Faculty@123',
        employeeNumber: '',
        designation: 'Assistant Professor',
        qualification: 'M.Tech (CSE)',
        specialization: 'Artificial Intelligence & Machine Learning',
        professionalExperience: '5+ Years',
        institutionalContact: 'ext: 4201',
        subjectId: sub1?.id || '',
        sectionId: sec1?.id || '',
        classTeacher: false,
        counsellor: false,
      });
    } catch (err: unknown) {
      setAddError(err instanceof Error ? err.message : 'Failed to create faculty');
    } finally {
      setIsSubmittingUser(false);
    }
  };

  const handleCreateOtherUser = async (e: React.FormEvent) => {
    e.preventDefault();
    const fullName = addForm.fullName?.trim();
    const username = addForm.username?.trim();
    const email = addForm.email?.trim();
    const password = addForm.password?.trim();

    if (!fullName || !username || !email || !password || !addForm.role) {
      setAddError('Please fill in all required fields');
      return;
    }

    setAddError(null);
    setIsSubmittingUser(true);
    try {
      const created = await userApi.createUser({
        ...addForm,
        fullName,
        username,
        email,
        password,
      } as CreateUserRequest);
      setUsers((prev) => [created, ...prev]);
      setShowAddUserModal(false);
      setAddForm({ role: 'HOD', classTeacher: false });
    } catch (err: unknown) {
      setAddError(err instanceof Error ? err.message : 'Failed to create user');
    } finally {
      setIsSubmittingUser(false);
    }
  };

  const openResponsibilitiesModal = async (u: UserSummary) => {
    setSelectedUserForResp(u);
    setLoadingResp(true);
    setRespError(null);
    setDeleteConfirmId(null);
    try {
      const resps = await responsibilityApi.getUserResponsibilities(u.id);
      setUserResponsibilities(resps);
      // Pre-fill department / section from metadata
      const cseDept = metadata?.departments.find((d) => d.code === 'CSE') || metadata?.departments[0];
      const aimlProg = metadata?.programs.find((p) => p.code === 'AIML') || metadata?.programs[0];
      const sec1 = metadata?.sections.find((s) => s.sectionName.includes('1')) || metadata?.sections[0];
      setRespForm({
        type: u.role === 'HOD' ? 'HOD_DEPT' : u.role === 'COUNSELLOR' ? 'COUNSELLOR' : 'TEACHING',
        departmentId: cseDept?.id,
        programId: aimlProg?.id,
        sectionId: sec1?.id,
      });
    } catch (err) {
      console.error('Failed to load user responsibilities', err);
    } finally {
      setLoadingResp(false);
    }
  };

  const handleAddResponsibility = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserForResp || !respForm.type) return;

    setRespError(null);
    setIsSubmittingResp(true);
    try {
      const created = await responsibilityApi.addResponsibility(
        selectedUserForResp.id,
        respForm as ResponsibilityRequest
      );
      setUserResponsibilities((prev) => [created, ...prev]);
      // Also update in parent users list
      setUsers((prev) =>
        prev.map((u) =>
          u.id === selectedUserForResp.id
            ? { ...u, responsibilities: [...u.responsibilities, created] }
            : u
        )
      );
      setRespForm((prev) => ({ ...prev, description: '' }));
    } catch (err: unknown) {
      setRespError(err instanceof Error ? err.message : 'Failed to assign responsibility');
    } finally {
      setIsSubmittingResp(false);
    }
  };

  const handleRemoveResponsibility = async (respId: string) => {
    try {
      await responsibilityApi.removeResponsibility(respId);
      // Update local responsibilities
      setUserResponsibilities((prev) =>
        prev.map((r) => (r.id === respId ? { ...r, status: 'INACTIVE' } : r))
      );
      // Update in parent users list
      if (selectedUserForResp) {
        setUsers((prev) =>
          prev.map((u) =>
            u.id === selectedUserForResp.id
              ? {
                  ...u,
                  responsibilities: u.responsibilities.map((r) =>
                    r.id === respId ? { ...r, status: 'INACTIVE' } : r
                  ),
                }
              : u
          )
        );
      }
      setDeleteConfirmId(null);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to remove responsibility');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center font-bold">
                <Users className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-xl font-bold text-slate-900">
                    User & Responsibility Management
                  </h1>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                    Downward Authority Active
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Manage accounts, assignment lifecycle (Teaching, Class Teacher, HOD, Counsellor), and access status.
                </p>
              </div>
            </div>

            <button
              onClick={() => {
                setAddForm({
                  role: allowedRolesToCreate[0] || 'FACULTY',
                  departmentId: metadata?.departments[0]?.id,
                  programId: metadata?.programs[0]?.id,
                  sectionId: metadata?.sections[0]?.id,
                  subjectId: metadata?.subjects[0]?.id,
                  classTeacher: false,
                });
                setShowAddUserModal(true);
              }}
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-sm transition-colors cursor-pointer self-start md:self-auto"
            >
              <UserPlus className="w-4 h-4" />
              Add New User
            </button>
          </div>
        </div>

        {/* Filters Bar */}
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-2xs mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="relative flex-1 max-w-md">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by name, username, or identifier..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center gap-2">
            <label className="text-xs text-slate-500 font-medium">Role:</label>
            <select
              value={selectedRoleFilter}
              onChange={(e) => setSelectedRoleFilter(e.target.value)}
              className="text-xs border border-slate-300 rounded-lg px-2.5 py-1.5 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="ALL">All Roles</option>
              <option value="PRINCIPAL">PRINCIPAL</option>
              <option value="HOD">HOD</option>
              <option value="FACULTY">FACULTY</option>
              <option value="CLASS_TEACHER">CLASS_TEACHER</option>
              <option value="COUNSELLOR">COUNSELLOR</option>
              <option value="STUDENT">STUDENT</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
        </div>

        {/* Users Table */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-2xs overflow-hidden">
          {loading ? (
            <div className="p-12 text-center text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mx-auto text-indigo-600 mb-2" />
              <p className="text-sm">Loading users and roles...</p>
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              <Users className="w-8 h-8 mx-auto text-slate-400 mb-2" />
              <p className="text-sm font-semibold text-slate-700">No users found</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4">User</th>
                    <th className="py-3 px-4">Role</th>
                    <th className="py-3 px-4">Identifiers</th>
                    <th className="py-3 px-4">Active Responsibilities</th>
                    <th className="py-3 px-4 text-center">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {filteredUsers.map((u) => {
                    const activeResps = u.responsibilities?.filter((r) => r.status === 'ACTIVE') || [];
                    return (
                      <tr key={u.id} className="hover:bg-slate-50 transition-colors">
                        <td className="py-3 px-4">
                          <div className="font-semibold text-slate-900">{u.fullName}</div>
                          <div className="text-[11px] text-slate-500 font-mono">{u.email}</div>
                          <div className="text-[10px] text-slate-400">@{u.username}</div>
                        </td>
                        <td className="py-3 px-4">
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3 px-4 font-mono text-[11px] text-slate-600">
                          {u.registrationNumber ? (
                            <div>Reg: <strong className="text-slate-800">{u.registrationNumber}</strong></div>
                          ) : null}
                          {u.uniqueId ? <div>ID: {u.uniqueId}</div> : null}
                          {!u.registrationNumber && !u.uniqueId ? (
                            <span className="text-slate-400">None</span>
                          ) : null}
                        </td>
                        <td className="py-3 px-4">
                          {activeResps.length > 0 ? (
                            <div className="flex flex-wrap gap-1 max-w-xs">
                              {activeResps.map((r) => (
                                <span
                                  key={r.id}
                                  className="px-2 py-0.5 rounded text-[10px] font-medium bg-indigo-50 text-indigo-700 border border-indigo-200"
                                >
                                  {r.type === 'CLASS_TEACHER'
                                    ? `Class Teacher (${r.sectionName || 'Sec 1'})`
                                    : r.type === 'TEACHING'
                                    ? `${r.subjectCode || 'DMGT'} (${r.sectionName || 'Sec 1'})`
                                    : r.type.replace('_', ' ')}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <span className="text-slate-400 text-[11px]">No active assignments</span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-center">
                          <span
                            className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                              u.enabled
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {u.enabled ? (
                              <>
                                <CheckCircle className="w-3 h-3 text-emerald-600" />
                                Enabled
                              </>
                            ) : (
                              <>
                                <XCircle className="w-3 h-3 text-red-600" />
                                Disabled
                              </>
                            )}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => openResponsibilitiesModal(u)}
                              className="px-2.5 py-1 text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded-md border border-indigo-200 transition-colors cursor-pointer"
                            >
                              Responsibilities
                            </button>
                            <button
                              onClick={() => handleToggleStatus(u)}
                              className={`px-2.5 py-1 text-xs font-semibold rounded-md border transition-colors cursor-pointer ${
                                u.enabled
                                  ? 'text-red-700 bg-red-50 hover:bg-red-100 border-red-200'
                                  : 'text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border-emerald-200'
                              }`}
                            >
                              {u.enabled ? 'Disable' : 'Enable'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>

      {/* Add User Modal */}
      {showAddUserModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 max-h-[92vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
              <div className="flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900">Add Academic DMS User</h3>
              </div>
              <button
                onClick={() => setShowAddUserModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Mode Tabs */}
            <div className="flex border-b border-slate-200 mb-4 text-xs font-bold">
              <button
                type="button"
                onClick={() => { setActiveAddTab('STUDENT'); setAddError(null); }}
                className={`pb-2.5 px-4 cursor-pointer transition-colors border-b-2 ${
                  activeAddTab === 'STUDENT'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                Add Student (With Prior Education)
              </button>
              <button
                type="button"
                onClick={() => { setActiveAddTab('FACULTY'); setAddError(null); }}
                className={`pb-2.5 px-4 cursor-pointer transition-colors border-b-2 ${
                  activeAddTab === 'FACULTY'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                Add Faculty (Professional & Assignments)
              </button>
              {(currentUser?.role === 'ADMIN' || currentUser?.role === 'PRINCIPAL') && (
                <button
                  type="button"
                  onClick={() => { setActiveAddTab('OTHER'); setAddError(null); }}
                  className={`pb-2.5 px-4 cursor-pointer transition-colors border-b-2 ${
                    activeAddTab === 'OTHER'
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-slate-500 hover:text-slate-800'
                  }`}
                >
                  Other Roles
                </button>
              )}
            </div>

            {addError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-xs flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <div>{addError}</div>
              </div>
            )}

            {/* 1. STUDENT ONBOARDING FLOW */}
            {activeAddTab === 'STUDENT' && (
              <form onSubmit={handleCreateStudent} className="space-y-4 text-xs">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Student Full Name *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Naga Kowshik"
                      value={studentForm.fullName}
                      onChange={(e) => setStudentForm({ ...studentForm, fullName: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Username *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. 25b81a4267"
                      value={studentForm.username}
                      onChange={(e) => setStudentForm({ ...studentForm, username: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Email *</label>
                    <input
                      type="email"
                      required
                      placeholder="student@crre.edu"
                      value={studentForm.email}
                      onChange={(e) => setStudentForm({ ...studentForm, email: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Initial Password *</label>
                    <div className="relative">
                      <input
                        type={showStudentPassword ? 'text' : 'password'}
                        required
                        value={studentForm.password}
                        onChange={(e) => setStudentForm({ ...studentForm, password: e.target.value })}
                        className="w-full pl-3 pr-10 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 font-mono text-xs"
                      />
                      <button
                        type="button"
                        onClick={() => setShowStudentPassword(!showStudentPassword)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                        title={showStudentPassword ? 'Hide password' : 'Show password'}
                      >
                        {showStudentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1">
                      Default: <code className="font-mono font-bold text-slate-700">Student@123</code> (can be customized)
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Roll / Reg Number *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. 25B81A4267"
                      value={studentForm.registrationNumber}
                      onChange={(e) => setStudentForm({ ...studentForm, registrationNumber: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Unique ID</label>
                    <input
                      type="text"
                      placeholder="e.g. CRRE25B81A4267"
                      value={studentForm.uniqueId}
                      onChange={(e) => setStudentForm({ ...studentForm, uniqueId: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Admission Type *</label>
                    <select
                      value={studentForm.entryType}
                      onChange={(e) => setStudentForm({ ...studentForm, entryType: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    >
                      <option value="REGULAR">REGULAR (SSC + Inter)</option>
                      <option value="LATERAL">LATERAL (Diploma)</option>
                    </select>
                  </div>
                </div>

                {/* Educational History Builder */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200">
                  <div className="flex items-center justify-between mb-3">
                    <span className="font-bold text-slate-800 text-xs">
                      Prior Educational Qualifications (History)
                    </span>
                    <button
                      type="button"
                      onClick={() =>
                        setStudentForm({
                          ...studentForm,
                          educationalHistory: [
                            ...studentForm.educationalHistory,
                            {
                              qualificationType: studentForm.entryType === 'LATERAL' ? 'DIPLOMA' : 'INTERMEDIATE',
                              boardOrUniversity: studentForm.entryType === 'LATERAL' ? 'SBTET' : 'BIEAP',
                              institutionName: '',
                              hallTicketNumber: '',
                              completionYear: 2024,
                              percentage: 80,
                            },
                          ],
                        })
                      }
                      className="text-xs text-indigo-600 hover:text-indigo-800 font-bold flex items-center gap-1 cursor-pointer"
                    >
                      <Plus className="w-3.5 h-3.5" /> Add Qualification
                    </button>
                  </div>

                  <div className="space-y-3">
                    {studentForm.educationalHistory.map((edu, idx) => (
                      <div key={idx} className="p-3 bg-white rounded-lg border border-slate-200 shadow-2xs">
                        <div className="flex items-center justify-between mb-2">
                          <span className="font-bold text-[11px] text-indigo-800">
                            Qualification #{idx + 1}
                          </span>
                          {studentForm.educationalHistory.length > 1 && (
                            <button
                              type="button"
                              onClick={() =>
                                setStudentForm({
                                  ...studentForm,
                                  educationalHistory: studentForm.educationalHistory.filter((_, i) => i !== idx),
                                })
                              }
                              className="text-red-500 hover:text-red-700 text-xs font-semibold cursor-pointer"
                            >
                              Remove
                            </button>
                          )}
                        </div>

                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">Type</label>
                            <select
                              value={edu.qualificationType}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].qualificationType = e.target.value;
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs"
                            >
                              <option value="SSC">SSC (10th)</option>
                              <option value="INTERMEDIATE">Intermediate (12th)</option>
                              <option value="DIPLOMA">Diploma (Polytechnic)</option>
                              <option value="OTHER">Other</option>
                            </select>
                          </div>

                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">Board / University</label>
                            <input
                              type="text"
                              required
                              placeholder="e.g. BSEAP"
                              value={edu.boardOrUniversity}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].boardOrUniversity = e.target.value;
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs"
                            />
                          </div>

                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">School / College</label>
                            <input
                              type="text"
                              required
                              placeholder="Institution name"
                              value={edu.institutionName}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].institutionName = e.target.value;
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs"
                            />
                          </div>

                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">Hall Ticket No.</label>
                            <input
                              type="text"
                              placeholder="e.g. HT-12345"
                              value={edu.hallTicketNumber}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].hallTicketNumber = e.target.value;
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs font-mono"
                            />
                          </div>

                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">Passing Year</label>
                            <input
                              type="number"
                              required
                              value={edu.completionYear}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].completionYear = Number(e.target.value);
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs"
                            />
                          </div>

                          <div>
                            <label className="block text-[11px] text-slate-600 mb-0.5">Percentage (%)</label>
                            <input
                              type="number"
                              step="0.01"
                              required
                              value={edu.percentage}
                              onChange={(e) => {
                                const updated = [...studentForm.educationalHistory];
                                updated[idx].percentage = Number(e.target.value);
                                setStudentForm({ ...studentForm, educationalHistory: updated });
                              }}
                              className="w-full p-1.5 border border-slate-300 rounded text-xs font-bold text-emerald-700"
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAddUserModal(false)}
                    className="px-4 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmittingUser}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
                  >
                    {isSubmittingUser && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    Enrol Student
                  </button>
                </div>
              </form>
            )}

            {/* 2. FACULTY ONBOARDING FLOW (NO PROGRAM FIELD) */}
            {activeAddTab === 'FACULTY' && (
              <form onSubmit={handleCreateFaculty} className="space-y-4 text-xs">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Faculty Full Name *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Dr. P. Seshagiri Rao"
                      value={facultyForm.fullName}
                      onChange={(e) => setFacultyForm({ ...facultyForm, fullName: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Username *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. seshagiri.cse"
                      value={facultyForm.username}
                      onChange={(e) => setFacultyForm({ ...facultyForm, username: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Email *</label>
                    <input
                      type="email"
                      required
                      placeholder="faculty@crre.edu"
                      value={facultyForm.email}
                      onChange={(e) => setFacultyForm({ ...facultyForm, email: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Initial Password *</label>
                    <div className="relative">
                      <input
                        type={showFacultyPassword ? 'text' : 'password'}
                        required
                        value={facultyForm.password}
                        onChange={(e) => setFacultyForm({ ...facultyForm, password: e.target.value })}
                        className="w-full pl-3 pr-10 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 font-mono text-xs"
                      />
                      <button
                        type="button"
                        onClick={() => setShowFacultyPassword(!showFacultyPassword)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                        title={showFacultyPassword ? 'Hide password' : 'Show password'}
                      >
                        {showFacultyPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1">
                      Default: <code className="font-mono font-bold text-slate-700">Faculty@123</code> (can be customized)
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Employee Number *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. E105"
                      value={facultyForm.employeeNumber}
                      onChange={(e) => setFacultyForm({ ...facultyForm, employeeNumber: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Designation *</label>
                    <select
                      value={facultyForm.designation}
                      onChange={(e) => setFacultyForm({ ...facultyForm, designation: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    >
                      <option value="Assistant Professor">Assistant Professor</option>
                      <option value="Associate Professor">Associate Professor</option>
                      <option value="Professor">Professor</option>
                      <option value="HOD">Head of Department</option>
                    </select>
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Department</label>
                    <div className="p-2 border border-slate-200 bg-slate-100 rounded-lg font-bold text-slate-800">
                      CSE (Institutional)
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Highest Qualification</label>
                    <input
                      type="text"
                      placeholder="e.g. M.Tech, Ph.D"
                      value={facultyForm.qualification}
                      onChange={(e) => setFacultyForm({ ...facultyForm, qualification: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Specialization</label>
                    <input
                      type="text"
                      placeholder="e.g. Machine Learning, Cloud"
                      value={facultyForm.specialization}
                      onChange={(e) => setFacultyForm({ ...facultyForm, specialization: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Experience</label>
                    <input
                      type="text"
                      placeholder="e.g. 8 Years"
                      value={facultyForm.professionalExperience}
                      onChange={(e) => setFacultyForm({ ...facultyForm, professionalExperience: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                    />
                  </div>
                </div>

                {/* Initial Teaching Assignment */}
                <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
                  <div className="font-bold text-slate-800 text-xs">
                    Initial Teaching Subject & Class Allocation
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Allocated Course / Subject</label>
                      <select
                        value={facultyForm.subjectId}
                        onChange={(e) => setFacultyForm({ ...facultyForm, subjectId: e.target.value })}
                        className="w-full p-2 border border-slate-300 rounded bg-white text-xs"
                      >
                        {metadata?.subjects.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.code} - {s.name} ({s.credits} Cr)
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Target Section</label>
                      <select
                        value={facultyForm.sectionId}
                        onChange={(e) => setFacultyForm({ ...facultyForm, sectionId: e.target.value })}
                        className="w-full p-2 border border-slate-300 rounded bg-white text-xs"
                      >
                        {metadata?.sections.map((sec) => (
                          <option key={sec.id} value={sec.id}>
                            {sec.sectionName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="pt-2 flex flex-col sm:flex-row gap-4">
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={facultyForm.classTeacher}
                        onChange={(e) => setFacultyForm({ ...facultyForm, classTeacher: e.target.checked })}
                        className="rounded text-teal-600 focus:ring-teal-500"
                      />
                      <span className="font-semibold text-slate-700">
                        Designate as Class Teacher for this Section
                      </span>
                    </label>

                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={facultyForm.counsellor}
                        onChange={(e) => setFacultyForm({ ...facultyForm, counsellor: e.target.checked })}
                        className="rounded text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="font-semibold text-slate-700">
                        Assign Academic Counsellor Mentorship
                      </span>
                    </label>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAddUserModal(false)}
                    className="px-4 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmittingUser}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
                  >
                    {isSubmittingUser && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    Add Faculty Member
                  </button>
                </div>
              </form>
            )}

            {/* 3. OTHER ROLES FLOW */}
            {activeAddTab === 'OTHER' && (
              <form onSubmit={handleCreateOtherUser} className="space-y-4 text-xs">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Full Name *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Dr. Institutional Head"
                      value={addForm.fullName || ''}
                      onChange={(e) => setAddForm({ ...addForm, fullName: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Username *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. hod.new"
                      value={addForm.username || ''}
                      onChange={(e) => setAddForm({ ...addForm, username: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Email *</label>
                    <input
                      type="email"
                      required
                      placeholder="head@crre.edu"
                      value={addForm.email || ''}
                      onChange={(e) => setAddForm({ ...addForm, email: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Password *</label>
                    <div className="relative">
                      <input
                        type={showOtherPassword ? 'text' : 'password'}
                        required
                        placeholder="e.g. Admin@123"
                        value={addForm.password || ''}
                        onChange={(e) => setAddForm({ ...addForm, password: e.target.value })}
                        className="w-full pl-3 pr-10 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 font-mono text-xs"
                      />
                      <button
                        type="button"
                        onClick={() => setShowOtherPassword(!showOtherPassword)}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 cursor-pointer"
                        title={showOtherPassword ? 'Hide password' : 'Show password'}
                      >
                        {showOtherPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1">
                      Minimum 6 characters. Make sure to remember or note down.
                    </p>
                  </div>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Role *</label>
                  <select
                    value={addForm.role}
                    onChange={(e) => setAddForm({ ...addForm, role: e.target.value as Role })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg font-semibold text-slate-800"
                  >
                    {allowedRolesToCreate.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAddUserModal(false)}
                    className="px-4 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmittingUser}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
                  >
                    {isSubmittingUser && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    Create Account
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Manage Responsibilities Modal */}
      {selectedUserForResp && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
              <div>
                <div className="flex items-center gap-2">
                  <Briefcase className="w-5 h-5 text-indigo-600" />
                  <h3 className="text-base font-bold text-slate-900">
                    Manage Responsibilities: {selectedUserForResp.fullName}
                  </h3>
                </div>
                <p className="text-xs text-slate-500 mt-0.5">
                  Role: <strong className="text-slate-700">{selectedUserForResp.role}</strong> &bull;
                  Username: <span className="font-mono">{selectedUserForResp.username}</span>
                </p>
              </div>
              <button
                onClick={() => setSelectedUserForResp(null)}
                className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Current Responsibilities List */}
            <div className="mb-6">
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Existing Assignments & Lifecycle
              </h4>

              {loadingResp ? (
                <div className="p-4 text-center text-slate-500 text-xs">
                  <Loader2 className="w-4 h-4 animate-spin mx-auto mb-1 text-indigo-600" />
                  Loading assignments...
                </div>
              ) : userResponsibilities.length === 0 ? (
                <div className="p-4 rounded-lg bg-slate-50 border border-slate-200 text-center text-xs text-slate-500">
                  No active or historical responsibilities assigned to this user yet.
                </div>
              ) : (
                <div className="space-y-2">
                  {userResponsibilities.map((r) => (
                    <div
                      key={r.id}
                      className={`p-3 rounded-lg border text-xs flex items-center justify-between gap-3 ${
                        r.status === 'ACTIVE'
                          ? 'bg-white border-slate-200 shadow-2xs'
                          : 'bg-slate-50 border-slate-200 text-slate-400 opacity-70'
                      }`}
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-slate-900">
                            {r.type.replace('_', ' ')}
                          </span>
                          <span
                            className={`px-1.5 py-0.2 rounded text-[10px] font-bold ${
                              r.status === 'ACTIVE'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-200 text-slate-600'
                            }`}
                          >
                            {r.status}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-600 mt-1">
                          {r.subjectName && <span>Subject: <strong>{r.subjectName}</strong> &bull; </span>}
                          {r.sectionName && <span>Section: <strong>{r.sectionName}</strong> &bull; </span>}
                          {r.departmentName && <span>Dept: <strong>{r.departmentName}</strong></span>}
                        </div>
                      </div>

                      {r.status === 'ACTIVE' && (
                        <div>
                          {deleteConfirmId === r.id ? (
                            <div className="flex items-center gap-1.5">
                              <button
                                onClick={() => handleRemoveResponsibility(r.id)}
                                className="px-2 py-1 bg-red-600 text-white rounded text-[10px] font-bold hover:bg-red-700 cursor-pointer"
                              >
                                Confirm Remove
                              </button>
                              <button
                                onClick={() => setDeleteConfirmId(null)}
                                className="px-2 py-1 bg-slate-200 text-slate-700 rounded text-[10px] hover:bg-slate-300 cursor-pointer"
                              >
                                Cancel
                              </button>
                            </div>
                          ) : (
                            <button
                              onClick={() => setDeleteConfirmId(r.id)}
                              title="Soft Remove Assignment"
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded border border-transparent hover:border-red-200 transition-colors cursor-pointer"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Note on data preservation */}
            <div className="mb-6 p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-[11px] leading-relaxed">
              <strong>Responsibility Mutation Guarantee:</strong> Adding, modifying, or soft-removing an assignment marks it inactive without deleting or recreating the user account, preserving all user profiles, academic timetables, and student enrollments.
            </div>

            {/* Add New Assignment Form */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200">
              <h4 className="text-xs font-bold text-slate-800 mb-3 flex items-center gap-1.5">
                <Plus className="w-4 h-4 text-indigo-600" />
                Assign New Responsibility
              </h4>

              {respError && (
                <div className="mb-3 p-2.5 bg-red-50 border border-red-200 rounded text-red-700 text-xs">
                  {respError}
                </div>
              )}

              <form onSubmit={handleAddResponsibility} className="space-y-3 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Assignment Type *
                  </label>
                  <select
                    value={respForm.type}
                    onChange={(e) => setRespForm({ ...respForm, type: e.target.value })}
                    className="w-full px-3 py-1.5 border border-slate-300 rounded bg-white font-semibold"
                  >
                    <option value="TEACHING">TEACHING (Subject & Section)</option>
                    <option value="CLASS_TEACHER">CLASS_TEACHER (Section Lead)</option>
                    <option value="HOD_DEPT">HOD_DEPT (Department Lead)</option>
                    <option value="COUNSELLOR">COUNSELLOR (Student Support)</option>
                    {currentUser?.role === 'ADMIN' && (
                      <option value="PRINCIPAL_INSTITUTION">PRINCIPAL_INSTITUTION</option>
                    )}
                  </select>
                </div>

                {respForm.type === 'TEACHING' && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Subject</label>
                      <select
                        value={respForm.subjectId || ''}
                        onChange={(e) => setRespForm({ ...respForm, subjectId: e.target.value })}
                        className="w-full px-2 py-1.5 border border-slate-300 rounded bg-white"
                      >
                        {metadata?.subjects.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.code} - {s.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Section</label>
                      <select
                        value={respForm.sectionId || ''}
                        onChange={(e) => setRespForm({ ...respForm, sectionId: e.target.value })}
                        className="w-full px-2 py-1.5 border border-slate-300 rounded bg-white"
                      >
                        {metadata?.sections.map((sec) => (
                          <option key={sec.id} value={sec.id}>
                            {sec.sectionName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}

                {respForm.type === 'CLASS_TEACHER' && (
                  <div>
                    <label className="block font-medium text-slate-600 mb-1">Section to Lead</label>
                    <select
                      value={respForm.sectionId || ''}
                      onChange={(e) => setRespForm({ ...respForm, sectionId: e.target.value })}
                      className="w-full px-2 py-1.5 border border-slate-300 rounded bg-white"
                    >
                      {metadata?.sections.map((sec) => (
                        <option key={sec.id} value={sec.id}>
                          {sec.sectionName}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {(respForm.type === 'HOD_DEPT' || respForm.type === 'COUNSELLOR') && (
                  <div>
                    <label className="block font-medium text-slate-600 mb-1">Department</label>
                    <select
                      value={respForm.departmentId || ''}
                      onChange={(e) => setRespForm({ ...respForm, departmentId: e.target.value })}
                      className="w-full px-2 py-1.5 border border-slate-300 rounded bg-white"
                    >
                      {metadata?.departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.code} - {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div>
                  <label className="block font-medium text-slate-600 mb-1">Notes / Description (Optional)</label>
                  <input
                    type="text"
                    placeholder="e.g. Primary instructor for AY 2025-26"
                    value={respForm.description || ''}
                    onChange={(e) => setRespForm({ ...respForm, description: e.target.value })}
                    className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white"
                  />
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isSubmittingResp}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
                  >
                    {isSubmittingResp && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    Assign Responsibility
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
