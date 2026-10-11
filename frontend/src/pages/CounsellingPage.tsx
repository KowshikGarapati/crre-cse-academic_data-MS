import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import { counsellingApi } from '../api/counselling.api';
import { studentApi } from '../api/student.api';
import type {
  CounsellingRecordDto,
  CounsellingStudentDto,
  CreateCounsellingRecordRequest,
  StudentCounsellingSummaryDto,
  StudentFullProfileDto,
} from '../types/auth.types';
import {
  HeartHandshake,
  Users,
  Calendar,
  PlusCircle,
  Search,
  AlertCircle,
  Loader2,
  FileText,
} from 'lucide-react';

export const CounsellingPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [mentees, setMentees] = useState<CounsellingStudentDto[]>([]);
  const [records, setRecords] = useState<CounsellingRecordDto[]>([]);
  const [studentProfile, setStudentProfile] = useState<StudentFullProfileDto | null>(null);
  const [studentSummary, setStudentSummary] = useState<StudentCounsellingSummaryDto | null>(null);

  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState<'mentees' | 'records'>('mentees');

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [meetingDate, setMeetingDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [recordType, setRecordType] = useState('ACADEMIC');
  const [remarks, setRemarks] = useState('');
  const [actionPlan, setActionPlan] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [status, setStatus] = useState('OPEN');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    setLoading(true);

    if (user.role === 'STUDENT') {
      Promise.all([
        studentApi.getMyProfile().catch(() => null),
        studentApi.getMyCounselling().catch(() => null),
      ])
        .then(([prof, summary]) => {
          setStudentProfile(prof);
          if (summary) {
            setStudentSummary(summary);
            const mappedRecords: CounsellingRecordDto[] = (summary.sessions || []).map((s) => ({
              id: s.id,
              counsellorName: summary.counsellorName || prof?.counsellorName || 'Assigned Counsellor',
              studentId: prof?.id || '',
              studentRegistrationNumber: prof?.registrationNumber || '',
              studentName: prof?.fullName || '',
              meetingDate: s.meetingDate,
              recordType: s.recordType,
              remarks: s.remarks,
              actionPlan: s.actionPlan,
              followUpDate: s.followUpDate,
              status: s.status,
              createdAt: s.meetingDate,
            }));
            setRecords(mappedRecords);
          } else {
            setRecords([]);
          }
        })
        .finally(() => setLoading(false));
    } else {
      Promise.all([
        counsellingApi.getMentees().catch(() => []),
        counsellingApi.getRecords().catch(() => []),
      ])
        .then(([mList, rList]) => {
          setMentees(Array.isArray(mList) ? mList : []);
          setRecords(Array.isArray(rList) ? rList : []);
        })
        .finally(() => setLoading(false));
    }
  }, [user]);

  const handleOpenLogModal = (studentId?: string) => {
    setSelectedStudentId(studentId || (mentees.length > 0 ? mentees[0].studentId : ''));
    setMeetingDate(new Date().toISOString().split('T')[0]);
    setRecordType('ACADEMIC');
    setRemarks('');
    setActionPlan('');
    setFollowUpDate('');
    setStatus('OPEN');
    setFormError(null);
    setShowModal(true);
  };

  const handleCreateRecord = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStudentId) {
      setFormError('Please select a student');
      return;
    }
    if (!remarks.trim()) {
      setFormError('Please enter session remarks');
      return;
    }

    setSubmitting(true);
    setFormError(null);
    try {
      const payload: CreateCounsellingRecordRequest = {
        studentId: selectedStudentId,
        meetingDate,
        recordType,
        remarks: remarks.trim(),
        actionPlan: actionPlan.trim() || undefined,
        followUpDate: followUpDate || undefined,
        status,
      };

      await counsellingApi.createRecord(payload);

      // Refresh data
      const [mList, rList] = await Promise.all([
        counsellingApi.getMentees().catch(() => []),
        counsellingApi.getRecords().catch(() => []),
      ]);
      setMentees(mList);
      setRecords(rList);

      setShowModal(false);
    } catch (err) {
      console.error('Failed to log counselling session', err);
      setFormError(err instanceof Error ? err.message : 'Failed to save record');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredMentees = useMemo(() => {
    if (!Array.isArray(mentees)) return [];
    const q = search.toLowerCase().trim();
    if (!q) return mentees;
    return mentees.filter(
      (m) =>
        m.fullName?.toLowerCase().includes(q) ||
        m.registrationNumber?.toLowerCase().includes(q) ||
        m.sectionName?.toLowerCase().includes(q)
    );
  }, [mentees, search]);

  const filteredRecords = useMemo(() => {
    if (!Array.isArray(records)) return [];
    const q = search.toLowerCase().trim();
    if (!q) return records;
    return records.filter(
      (r) =>
        r.studentName?.toLowerCase().includes(q) ||
        r.studentRegistrationNumber?.toLowerCase().includes(q) ||
        r.recordType?.toLowerCase().includes(q) ||
        r.remarks?.toLowerCase().includes(q)
    );
  }, [records, search]);

  if (!user) return null;

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header Banner */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-2xs mb-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold shadow-sm">
                <HeartHandshake className="w-7 h-7" />
              </div>
              <div>
                <div className="flex items-center gap-2.5">
                  <h1 className="text-2xl font-black text-slate-900 tracking-tight">
                    {user.role === 'STUDENT'
                      ? 'My Academic Mentorship & Counselling'
                      : 'Student Mentorship & Counselling Workspace'}
                  </h1>
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  {user.role === 'STUDENT'
                    ? 'Confidential guidance, progress tracking, and academic support'
                    : 'Designated Counsellor & Mentor Scope &bull; Confidential Student Advisory'}
                </p>
              </div>
            </div>

            {user.role !== 'STUDENT' && mentees.length > 0 && (
              <button
                onClick={() => handleOpenLogModal()}
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs transition-colors shadow-2xs cursor-pointer"
              >
                <PlusCircle className="w-4 h-4" />
                Log Counselling Session
              </button>
            )}
          </div>

          {/* Metrics summary */}
          {user.role !== 'STUDENT' && (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-slate-100">
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/70">
                <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
                  Assigned Mentees
                </div>
                <div className="text-xl font-extrabold text-slate-900 mt-0.5">
                  {mentees.length} Students
                </div>
              </div>
              <div className="p-3.5 rounded-xl bg-indigo-50/70 border border-indigo-200/70">
                <div className="text-[11px] font-semibold text-indigo-700 uppercase tracking-wider">
                  Total Sessions Logged
                </div>
                <div className="text-xl font-extrabold text-indigo-900 mt-0.5">
                  {records.length} Sessions
                </div>
              </div>
              <div className="p-3.5 rounded-xl bg-teal-50/70 border border-teal-200/70">
                <div className="text-[11px] font-semibold text-teal-700 uppercase tracking-wider">
                  Open / In Progress
                </div>
                <div className="text-xl font-extrabold text-teal-900 mt-0.5">
                  {records.filter((r) => r.status === 'OPEN' || r.status === 'FOLLOW_UP').length} Action Plans
                </div>
              </div>
            </div>
          )}

          {user.role === 'STUDENT' && (
            <div className="mt-6 pt-6 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-slate-600">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-500">Designated Mentor:</span>
                <span className="px-2.5 py-1 rounded bg-indigo-50 text-indigo-800 font-bold border border-indigo-200">
                  {studentSummary?.counsellorName || studentProfile?.counsellorName || 'Assigned Mentor'}
                </span>
                {(studentSummary?.counsellorEmail || studentProfile?.counsellorEmail) && (
                  <span className="text-slate-400">({studentSummary?.counsellorEmail || studentProfile?.counsellorEmail})</span>
                )}
              </div>
              <div>
                Total Sessions on Record:{' '}
                <strong className="text-indigo-700 font-bold">{records.length}</strong>
              </div>
            </div>
          )}
        </div>

        {loading ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center">
            <Loader2 className="w-8 h-8 text-indigo-600 animate-spin mx-auto mb-2" />
            <p className="text-xs text-slate-500 font-medium">Loading Counselling Records...</p>
          </div>
        ) : user.role === 'STUDENT' ? (
          /* ================= STUDENT VIEW ================= */
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs p-6">
            <h3 className="text-base font-bold text-slate-900 mb-4 flex items-center gap-2">
              <FileText className="w-5 h-5 text-indigo-600" />
              Recorded Counselling Sessions & Action Plans
            </h3>

            {records.length === 0 ? (
              <div className="text-center py-12 text-slate-500 text-xs">
                No counselling sessions recorded yet.
              </div>
            ) : (
              <div className="space-y-4">
                {records.map((rec) => (
                  <div key={rec.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50/60">
                    <div className="flex items-center justify-between mb-2">
                      <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">
                        {rec.recordType}
                      </span>
                      <span className="text-xs text-slate-500 flex items-center gap-1">
                        <Calendar className="w-3.5 h-3.5" /> Meeting Date: {rec.meetingDate}
                      </span>
                    </div>
                    <p className="text-xs text-slate-800 font-medium mb-2">
                      <strong>Remarks:</strong> {rec.remarks}
                    </p>
                    {rec.actionPlan && (
                      <p className="text-xs text-slate-600 mb-2">
                        <strong>Action Plan:</strong> {rec.actionPlan}
                      </p>
                    )}
                    <div className="pt-2 border-t border-slate-200 text-[11px] text-slate-500 flex justify-between">
                      <span>Status: <strong className="uppercase font-bold text-slate-700">{rec.status}</strong></span>
                      {rec.followUpDate && <span>Follow-up Date: {rec.followUpDate}</span>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          /* ================= FACULTY / COUNSELLOR VIEW ================= */
          <div>
            {/* Tab Controls */}
            <div className="flex border-b border-slate-200 mb-6 gap-2">
              <button
                onClick={() => setActiveTab('mentees')}
                className={`px-4 py-2.5 text-xs font-bold border-b-2 flex items-center gap-2 transition-colors cursor-pointer ${
                  activeTab === 'mentees'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-900'
                }`}
              >
                <Users className="w-4 h-4" /> Assigned Mentees ({mentees.length})
              </button>
              <button
                onClick={() => setActiveTab('records')}
                className={`px-4 py-2.5 text-xs font-bold border-b-2 flex items-center gap-2 transition-colors cursor-pointer ${
                  activeTab === 'records'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-900'
                }`}
              >
                <FileText className="w-4 h-4" /> Session Logs & Action Plans ({records.length})
              </button>
            </div>

            {/* TAB 1: ASSIGNED MENTEES */}
            {activeTab === 'mentees' && (
              <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
                <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50/50">
                  <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search mentees by roll number, name, or section..."
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>
                  <span className="text-xs text-slate-500 font-medium">
                    Showing {filteredMentees.length} of {mentees.length} mentees
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                      <tr>
                        <th className="py-3 px-4">Roll Number</th>
                        <th className="py-3 px-4">Student Name</th>
                        <th className="py-3 px-4">Section</th>
                        <th className="py-3 px-4">Program</th>
                        <th className="py-3 px-4 text-center">Sessions</th>
                        <th className="py-3 px-4">Last Meeting</th>
                        <th className="py-3 px-4">Status</th>
                        <th className="py-3 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredMentees.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="py-8 text-center text-slate-500">
                            {mentees.length === 0
                              ? 'No mentees are assigned to you yet.'
                              : `No mentees match "${search}"`}
                          </td>
                        </tr>
                      ) : (
                        filteredMentees.map((m) => (
                          <tr
                            key={m.studentId}
                            className="hover:bg-indigo-50/60 transition-colors group cursor-pointer"
                            onClick={() => navigate(`/students/${m.studentId}`)}
                          >
                            <td className="py-3 px-4 font-mono font-bold text-indigo-900 group-hover:text-indigo-700">
                              {m.registrationNumber}
                            </td>
                            <td className="py-3 px-4 font-semibold text-slate-800 group-hover:text-indigo-950">
                              {m.fullName}
                            </td>
                            <td className="py-3 px-4 text-slate-600 font-medium">{m.sectionName}</td>
                            <td className="py-3 px-4 text-slate-500">{m.programName}</td>
                            <td className="py-3 px-4 text-center font-bold text-indigo-700">
                              {m.totalSessions}
                            </td>
                            <td className="py-3 px-4 text-slate-600">
                              {m.lastMeetingDate || 'None'}
                            </td>
                            <td className="py-3 px-4">
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  m.lastStatus === 'OPEN'
                                    ? 'bg-amber-100 text-amber-800'
                                    : 'bg-slate-100 text-slate-700'
                                }`}
                              >
                                {m.lastStatus}
                              </span>
                            </td>
                            <td className="py-3 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                              <button
                                onClick={() => handleOpenLogModal(m.studentId)}
                                className="px-2.5 py-1 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-[11px] transition-colors cursor-pointer"
                              >
                                + Log Session
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* TAB 2: SESSION LOGS */}
            {activeTab === 'records' && (
              <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
                <div className="p-4 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50/50">
                  <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search session logs by student, type, or remarks..."
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                    />
                  </div>
                  <span className="text-xs text-slate-500 font-medium">
                    Showing {filteredRecords.length} of {records.length} records
                  </span>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                      <tr>
                        <th className="py-3 px-4">Date</th>
                        <th className="py-3 px-4">Roll Number</th>
                        <th className="py-3 px-4">Student Name</th>
                        <th className="py-3 px-4">Category</th>
                        <th className="py-3 px-4">Remarks & Action Plan</th>
                        <th className="py-3 px-4">Follow-Up</th>
                        <th className="py-3 px-4">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredRecords.length === 0 ? (
                        <tr>
                          <td colSpan={7} className="py-8 text-center text-slate-500">
                            {records.length === 0
                              ? 'No session records logged yet.'
                              : `No session records match "${search}"`}
                          </td>
                        </tr>
                      ) : (
                        filteredRecords.map((r) => (
                          <tr key={r.id} className="hover:bg-slate-50/70">
                            <td className="py-3 px-4 font-mono text-slate-600 whitespace-nowrap">
                              {r.meetingDate}
                            </td>
                            <td className="py-3 px-4 font-mono font-bold text-indigo-900 whitespace-nowrap">
                              {r.studentRegistrationNumber}
                            </td>
                            <td className="py-3 px-4 font-semibold text-slate-800 whitespace-nowrap">
                              {r.studentName}
                            </td>
                            <td className="py-3 px-4 whitespace-nowrap">
                              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-100 text-indigo-800">
                                {r.recordType}
                              </span>
                            </td>
                            <td className="py-3 px-4 max-w-xs">
                              <div className="font-medium text-slate-800">{r.remarks}</div>
                              {r.actionPlan && (
                                <div className="text-[11px] text-slate-500 mt-0.5 italic">
                                  Plan: {r.actionPlan}
                                </div>
                              )}
                            </td>
                            <td className="py-3 px-4 font-mono text-slate-500 whitespace-nowrap">
                              {r.followUpDate || '-'}
                            </td>
                            <td className="py-3 px-4 whitespace-nowrap">
                              <span
                                className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                  r.status === 'OPEN'
                                    ? 'bg-amber-100 text-amber-800'
                                    : 'bg-emerald-100 text-emerald-800'
                                }`}
                              >
                                {r.status}
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
          </div>
        )}

        {/* LOG COUNSELLING SESSION MODAL */}
        {showModal && (
          <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-4">
                <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                  <HeartHandshake className="w-5 h-5 text-indigo-600" />
                  Log Counselling Session
                </h3>
                <button
                  onClick={() => setShowModal(false)}
                  className="text-slate-400 hover:text-slate-600 text-lg font-bold"
                >
                  &times;
                </button>
              </div>

              {formError && (
                <div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <form onSubmit={handleCreateRecord} className="space-y-4 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Select Mentee Student <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={selectedStudentId}
                    onChange={(e) => setSelectedStudentId(e.target.value)}
                    className="w-full p-2 border border-slate-300 rounded-lg bg-white"
                  >
                    {mentees.map((m) => (
                      <option key={m.studentId} value={m.studentId}>
                        {m.registrationNumber} - {m.fullName} ({m.sectionName})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Meeting Date <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="date"
                      value={meetingDate}
                      onChange={(e) => setMeetingDate(e.target.value)}
                      className="w-full p-2 border border-slate-300 rounded-lg bg-white"
                      required
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Category <span className="text-rose-500">*</span>
                    </label>
                    <select
                      value={recordType}
                      onChange={(e) => setRecordType(e.target.value)}
                      className="w-full p-2 border border-slate-300 rounded-lg bg-white"
                    >
                      <option value="ACADEMIC">ACADEMIC</option>
                      <option value="CAREER">CAREER</option>
                      <option value="ATTENDANCE">ATTENDANCE</option>
                      <option value="PERSONAL">PERSONAL</option>
                      <option value="GENERAL">GENERAL</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Session Remarks / Notes <span className="text-rose-500">*</span>
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Enter discussion notes, student challenges, guidance provided..."
                    value={remarks}
                    onChange={(e) => setRemarks(e.target.value)}
                    className="w-full p-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500"
                    required
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Action Plan / Recommendations
                  </label>
                  <textarea
                    rows={2}
                    placeholder="Steps agreed upon for student improvement..."
                    value={actionPlan}
                    onChange={(e) => setActionPlan(e.target.value)}
                    className="w-full p-2 border border-slate-300 rounded-lg"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Follow-Up Date
                    </label>
                    <input
                      type="date"
                      value={followUpDate}
                      onChange={(e) => setFollowUpDate(e.target.value)}
                      className="w-full p-2 border border-slate-300 rounded-lg bg-white"
                    />
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">
                      Status
                    </label>
                    <select
                      value={status}
                      onChange={(e) => setStatus(e.target.value)}
                      className="w-full p-2 border border-slate-300 rounded-lg bg-white"
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="FOLLOW_UP">FOLLOW UP</option>
                      <option value="RESOLVED">RESOLVED</option>
                    </select>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowModal(false)}
                    className="px-3.5 py-2 rounded-lg border border-slate-200 text-slate-600 font-semibold hover:bg-slate-50 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-bold transition-colors cursor-pointer flex items-center gap-1.5"
                  >
                    {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    Save Record
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};
