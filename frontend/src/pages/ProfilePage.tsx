import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import {
  User as UserIcon,
  Mail,
  Shield,
  Hash,
  CreditCard,
  BookOpen,
  ArrowLeft,
  LogOut,
  CheckCircle,
} from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const formatAadhaar = (num?: number | null) => {
    if (!num) return 'Not Provided';
    const s = num.toString().padStart(12, '0');
    return `${s.slice(0, 4)} ${s.slice(4, 8)} ${s.slice(8, 12)}`;
  };

  const formatApaar = (num?: number | null) => {
    if (!num) return 'Not Provided';
    const s = num.toString().padStart(12, '0');
    return `${s.slice(0, 4)} ${s.slice(4, 8)} ${s.slice(8, 12)}`;
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-4xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-6 flex items-center justify-between">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-indigo-600 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>

          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-red-600 hover:bg-red-50 border border-red-200 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            Logout
          </button>
        </div>

        {/* Profile Card */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          {/* Header Banner */}
          <div className="bg-gradient-to-r from-indigo-600 to-indigo-800 p-6 sm:p-8 text-white">
            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
              <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center text-white text-2xl font-bold border border-white/30">
                {user.fullName.charAt(0)}
              </div>
              <div>
                <h1 className="text-2xl font-extrabold">{user.fullName}</h1>
                <div className="flex flex-wrap items-center gap-2 mt-1.5">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-white/20 text-white border border-white/30">
                    {user.role}
                  </span>
                  <span className="text-xs text-indigo-100 flex items-center gap-1">
                    <CheckCircle className="w-3.5 h-3.5 text-emerald-300" />
                    Account Active
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Details Section */}
          <div className="p-6 sm:p-8 space-y-6">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-4">
                Account Information
              </h2>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <UserIcon className="w-3.5 h-3.5 text-slate-400" />
                    Username
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900 font-mono">
                    {user.username}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Mail className="w-3.5 h-3.5 text-slate-400" />
                    Email Address
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900">{user.email}</dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Shield className="w-3.5 h-3.5 text-slate-400" />
                    Role
                  </dt>
                  <dd className="mt-1 text-sm font-semibold text-slate-900">{user.role}</dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-2">
                    <Hash className="w-3.5 h-3.5 text-slate-400" />
                    User UUID
                  </dt>
                  <dd className="mt-1 text-xs font-mono font-semibold text-slate-700 truncate">
                    {user.id}
                  </dd>
                </div>
              </dl>
            </div>

            <div className="border-t border-slate-200 pt-6">
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-400 mb-4">
                Institutional & Government Identifiers
              </h2>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium">
                    College Registration Number (~10 chars)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {user.registrationNumber || <span className="text-slate-400 font-normal">N/A</span>}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium">
                    Institutional Unique ID (~12 chars)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {user.uniqueId || <span className="text-slate-400 font-normal">N/A</span>}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-1.5">
                    <CreditCard className="w-3.5 h-3.5 text-slate-400" />
                    Aadhaar Number (12 Digits)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {formatAadhaar(user.aadharNumber)}
                  </dd>
                </div>

                <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-100">
                  <dt className="text-xs text-slate-500 font-medium flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5 text-slate-400" />
                    APAAR ID (12 Digits)
                  </dt>
                  <dd className="mt-1 text-sm font-mono font-bold text-slate-900">
                    {formatApaar(user.apaarNumber)}
                  </dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
