import React, { useEffect, useState } from 'react';
import { studentApi } from '../api/student.api';
import type {
  AcademicMetadata,
  StudentDetailDto,
  UpdateStudentRequest,
} from '../types/auth.types';
import {
  X,
  UserCheck,
  AlertTriangle,
  Loader2,
  CheckCircle2,
  Building,
  Mail,
  User,
  Shield,
} from 'lucide-react';

interface StudentEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  studentId: string;
  initialData?: StudentDetailDto | null;
  metadata?: AcademicMetadata | null;
  onSaved: (updated: StudentDetailDto) => void;
}

export const StudentEditModal: React.FC<StudentEditModalProps> = ({
  isOpen,
  onClose,
  studentId,
  initialData,
  metadata,
  onSaved,
}) => {
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [entryType, setEntryType] = useState<'REGULAR' | 'LATERAL'>('REGULAR');
  const [admissionCategory, setAdmissionCategory] = useState<'CONVENOR' | 'MANAGEMENT' | ''>('');
  const [tuitionCategory, setTuitionCategory] = useState<'RTF' | 'SELF_FINANCE' | ''>('');
  const [aadharNumber, setAadharNumber] = useState<string>('');
  const [apaarNumber, setApaarNumber] = useState<string>('');
  const [sectionId, setSectionId] = useState<string>('');
  const [status, setStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE');
  const [registrationNumber, setRegistrationNumber] = useState<string>('');

  useEffect(() => {
    if (!isOpen || !studentId) return;

    setError(null);
    setSuccess(null);

    const populateForm = (data: StudentDetailDto) => {
      setFullName(data.fullName || '');
      setEmail(data.email || '');
      setEntryType((data.entryType as 'REGULAR' | 'LATERAL') || 'REGULAR');
      setAdmissionCategory((data.admissionCategory as 'CONVENOR' | 'MANAGEMENT') || '');
      setTuitionCategory((data.tuitionCategory as 'RTF' | 'SELF_FINANCE') || '');
      setAadharNumber(data.aadharNumber ? String(data.aadharNumber) : '');
      setApaarNumber(data.apaarNumber ? String(data.apaarNumber) : '');
      setSectionId(data.placement?.sectionId || '');
      setStatus((data.status as 'ACTIVE' | 'INACTIVE') || 'ACTIVE');
      setRegistrationNumber(data.registrationNumber || '');
    };

    if (initialData && (initialData.id === studentId || initialData.registrationNumber === studentId)) {
      populateForm(initialData);
    } else {
      setLoading(true);
      studentApi
        .getStudentDetails(studentId)
        .then((data) => {
          populateForm(data);
        })
        .catch((err) => {
          setError(err instanceof Error ? err.message : 'Failed to load student details for editing.');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [isOpen, studentId, initialData]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    const trimmedName = fullName.trim();
    if (!trimmedName || trimmedName.length < 2 || trimmedName.length > 100) {
      setError('Student full name must be between 2 and 100 characters.');
      return;
    }

    if (aadharNumber && !/^\d{12}$/.test(aadharNumber.trim())) {
      setError('Aadhar number must be exactly 12 digits if provided.');
      return;
    }

    if (apaarNumber && !/^\d{12}$/.test(apaarNumber.trim())) {
      setError('APAAR number must be exactly 12 digits if provided.');
      return;
    }

    const payload: UpdateStudentRequest = {
      fullName: trimmedName,
      email: email.trim() || undefined,
      entryType,
      admissionCategory: admissionCategory ? admissionCategory : null,
      tuitionCategory: tuitionCategory ? tuitionCategory : null,
      aadharNumber: aadharNumber.trim() ? Number(aadharNumber.trim()) : null,
      apaarNumber: apaarNumber.trim() ? Number(apaarNumber.trim()) : null,
      sectionId: sectionId || null,
      status,
    };

    setSubmitting(true);
    try {
      const updated = await studentApi.updateStudent(studentId, payload);
      setSuccess('Student profile updated successfully.');
      onSaved(updated);
      setTimeout(() => {
        onClose();
      }, 700);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to update student profile.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-5">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <UserCheck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900">
                Edit Student Profile
              </h3>
              <p className="text-xs text-slate-500 font-mono">
                Roll No: <span className="font-bold text-slate-800">{registrationNumber || studentId}</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs flex items-start gap-2">
            <AlertTriangle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Update Failed</p>
              <p className="mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{success}</span>
          </div>
        )}

        {loading ? (
          <div className="p-10 text-center text-slate-500">
            <Loader2 className="w-6 h-6 animate-spin mx-auto text-indigo-600 mb-2" />
            <p className="text-xs font-semibold">Loading student information...</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4 text-xs">
            {/* Primary Profile Details */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1">
                  <User className="w-3.5 h-3.5 text-slate-400" /> Full Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. John Doe"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1">
                  <Mail className="w-3.5 h-3.5 text-slate-400" /> Institutional Email
                </label>
                <input
                  type="email"
                  placeholder="e.g. student@crre.edu.in"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
            </div>

            {/* Academic Admission & Tuition Classification */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Entry Type *
                </label>
                <select
                  value={entryType}
                  onChange={(e) => setEntryType(e.target.value as 'REGULAR' | 'LATERAL')}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="REGULAR">REGULAR</option>
                  <option value="LATERAL">LATERAL</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Admission Category
                </label>
                <select
                  value={admissionCategory}
                  onChange={(e) =>
                    setAdmissionCategory(e.target.value as 'CONVENOR' | 'MANAGEMENT' | '')
                  }
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="">(Not Specified)</option>
                  <option value="CONVENOR">CONVENOR</option>
                  <option value="MANAGEMENT">MANAGEMENT</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Tuition Category
                </label>
                <select
                  value={tuitionCategory}
                  onChange={(e) =>
                    setTuitionCategory(e.target.value as 'RTF' | 'SELF_FINANCE' | '')
                  }
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="">(Not Specified)</option>
                  <option value="RTF">RTF</option>
                  <option value="SELF_FINANCE">SELF_FINANCE</option>
                </select>
              </div>
            </div>

            {/* Government Identifiers (Aadhar & APAAR) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1">
                  <Shield className="w-3.5 h-3.5 text-slate-400" /> Aadhar Number (12 Digits)
                </label>
                <input
                  type="text"
                  maxLength={12}
                  placeholder="e.g. 123456789012"
                  value={aadharNumber}
                  onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '');
                    setAadharNumber(val);
                  }}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1">
                  <Shield className="w-3.5 h-3.5 text-slate-400" /> APAAR Number (12 Digits)
                </label>
                <input
                  type="text"
                  maxLength={12}
                  placeholder="e.g. 987654321098"
                  value={apaarNumber}
                  onChange={(e) => {
                    const val = e.target.value.replace(/\D/g, '');
                    setApaarNumber(val);
                  }}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Section & Status */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block font-semibold text-slate-700 mb-1 flex items-center gap-1">
                  <Building className="w-3.5 h-3.5 text-slate-400" /> Academic Section
                </label>
                <select
                  value={sectionId}
                  onChange={(e) => setSectionId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="">Keep Current Section</option>
                  {metadata?.sections?.map((sec) => (
                    <option key={sec.id} value={sec.id}>
                      {sec.sectionName} {sec.programCode ? `(${sec.programCode})` : ''}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-400 mt-1">
                  Re-assigning section adjusts the student's timetable and class teacher scope.
                </p>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Enrollment Status
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as 'ACTIVE' | 'INACTIVE')}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white font-semibold"
                >
                  <option value="ACTIVE">ACTIVE (Enrolled & Attending)</option>
                  <option value="INACTIVE">INACTIVE (Discontinued / Suspended)</option>
                </select>
              </div>
            </div>

            {/* Note on security boundary */}
            <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-slate-500 text-[11px] leading-relaxed">
              <strong>Institutional Boundary Notice:</strong> Roll numbers, user credentials, and security roles are managed through institution administration and cannot be modified here.
            </div>

            {/* Modal Actions */}
            <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 border border-slate-300 text-slate-700 font-semibold rounded-lg hover:bg-slate-50 cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg shadow-sm disabled:opacity-60 cursor-pointer flex items-center gap-1.5"
              >
                {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                Save Changes
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
