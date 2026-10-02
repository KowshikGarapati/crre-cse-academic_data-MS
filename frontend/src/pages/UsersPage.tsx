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
  const [addForm, setAddForm] = useState<Partial<CreateUserRequest>>({
    role: 'FACULTY',
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

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!addForm.fullName || !addForm.username || !addForm.email || !addForm.password || !addForm.role) {
      setAddError('Please fill in all required fields');
      return;
    }

    setAddError(null);
    setIsSubmittingUser(true);
    try {
      const created = await userApi.createUser(addForm as CreateUserRequest);
      setUsers((prev) => [created, ...prev]);
      setShowAddUserModal(false);
      setAddForm({ role: 'FACULTY', classTeacher: false });
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
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
              <div className="flex items-center gap-2">
                <UserPlus className="w-5 h-5 text-indigo-600" />
                <h3 className="text-base font-bold text-slate-900">Add New User</h3>
              </div>
              <button
                onClick={() => setShowAddUserModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {addError && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-xs flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <div>{addError}</div>
              </div>
            )}

            <form onSubmit={handleCreateUser} className="space-y-4 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Full Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Dr. Rajesh Kumar"
                    value={addForm.fullName || ''}
                    onChange={(e) => setAddForm({ ...addForm, fullName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Username *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. rajesh.cse"
                    value={addForm.username || ''}
                    onChange={(e) => setAddForm({ ...addForm, username: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Email *</label>
                  <input
                    type="email"
                    required
                    placeholder="user@crre.edu"
                    value={addForm.email || ''}
                    onChange={(e) => setAddForm({ ...addForm, email: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Password *</label>
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={addForm.password || ''}
                    onChange={(e) => setAddForm({ ...addForm, password: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Role *</label>
                <select
                  value={addForm.role}
                  onChange={(e) => setAddForm({ ...addForm, role: e.target.value as Role })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 font-semibold text-slate-800"
                >
                  {allowedRolesToCreate.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
                {currentUser?.role === 'HOD' && (
                  <p className="mt-1 text-[11px] text-slate-500">
                    Downward Authority: HOD can create Faculty, Counsellor, and Student accounts.
                  </p>
                )}
              </div>

              {/* Dynamic Role Fields */}
              {(addForm.role === 'FACULTY' || addForm.role === 'CLASS_TEACHER') && (
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-3">
                  <div className="font-bold text-slate-800 text-[11px]">
                    Initial Faculty Academic Assignment
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Course / Subject</label>
                      <select
                        value={addForm.subjectId || ''}
                        onChange={(e) => setAddForm({ ...addForm, subjectId: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white"
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
                        value={addForm.sectionId || ''}
                        onChange={(e) => setAddForm({ ...addForm, sectionId: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white"
                      >
                        {metadata?.sections.map((sec) => (
                          <option key={sec.id} value={sec.id}>
                            {sec.sectionName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="pt-2">
                    <label className="inline-flex items-center gap-2 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={addForm.classTeacher || addForm.role === 'CLASS_TEACHER'}
                        onChange={(e) => setAddForm({ ...addForm, classTeacher: e.target.checked })}
                        className="rounded text-indigo-600 focus:ring-indigo-500"
                      />
                      <span className="font-semibold text-slate-700">
                        Designate as Class Teacher for this Section
                      </span>
                    </label>
                  </div>
                </div>
              )}

              {addForm.role === 'STUDENT' && (
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 space-y-3">
                  <div className="font-bold text-slate-800 text-[11px]">
                    Student Enrollment Details
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Roll / Reg Number (~10 chars)</label>
                      <input
                        type="text"
                        placeholder="e.g. 25B81A4267"
                        value={addForm.registrationNumber || ''}
                        onChange={(e) => setAddForm({ ...addForm, registrationNumber: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white font-mono"
                      />
                    </div>
                    <div>
                      <label className="block font-medium text-slate-600 mb-1">Unique ID (~12 chars)</label>
                      <input
                        type="text"
                        placeholder="e.g. CRRE20244267"
                        value={addForm.uniqueId || ''}
                        onChange={(e) => setAddForm({ ...addForm, uniqueId: e.target.value })}
                        className="w-full px-2.5 py-1.5 border border-slate-300 rounded bg-white font-mono"
                      />
                    </div>
                  </div>
                </div>
              )}

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
                  Create User Account
                </button>
              </div>
            </form>
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
