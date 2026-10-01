import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Navbar } from '../components/Navbar';
import {
  ShieldCheck,
  IdCard,
  User,
  ArrowRight,
  Database,
  CheckCircle2,
  FileText,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  if (!user) return null;

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-indigo-700 via-indigo-800 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg mb-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-sm text-xs font-semibold text-indigo-100 border border-white/20 mb-3">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                V1 Authentication Active
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                Welcome, {user.fullName}
              </h1>
              <p className="mt-1 text-sm sm:text-base text-indigo-200">
                Logged in as <span className="font-semibold text-white">{user.role}</span> via{' '}
                <span className="font-mono text-indigo-100">{user.username}</span>
              </p>
            </div>

            <Link
              to="/profile"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-white text-indigo-900 font-semibold text-sm hover:bg-indigo-50 transition-colors shadow-sm self-start md:self-auto"
            >
              <User className="w-4 h-4" />
              View Full Profile
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>

        {/* Overview Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Identity Card */}
          <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-lg">
                <IdCard className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Institutional Identity</h3>
                <p className="text-xs text-slate-500">College academic identifiers</p>
              </div>
            </div>
            <div className="space-y-2.5 text-xs">
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500 font-medium">Registration No</span>
                <span className="font-mono font-semibold text-slate-800">
                  {user.registrationNumber || 'N/A'}
                </span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500 font-medium">Unique ID</span>
                <span className="font-mono font-semibold text-slate-800">
                  {user.uniqueId || 'N/A'}
                </span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500 font-medium">APAAR ID</span>
                <span className="font-mono font-semibold text-slate-800">
                  {user.apaarNumber || 'N/A'}
                </span>
              </div>
            </div>
          </div>

          {/* Platform Status */}
          <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Security & Session</h3>
                <p className="text-xs text-slate-500">JWT Authentication</p>
              </div>
            </div>
            <div className="space-y-2.5 text-xs">
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500 font-medium">Session Status</span>
                <span className="inline-flex items-center gap-1 font-semibold text-emerald-600">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  Active / Valid
                </span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-100">
                <span className="text-slate-500 font-medium">Token Type</span>
                <span className="font-mono font-semibold text-slate-800">Bearer (HMAC-SHA256)</span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-500 font-medium">Role Access</span>
                <span className="font-semibold text-indigo-600">{user.role}</span>
              </div>
            </div>
          </div>

          {/* SIMS Integration Note */}
          <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-2.5 bg-amber-50 text-amber-600 rounded-lg">
                <Database className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Architecture Scope</h3>
                <p className="text-xs text-slate-500">SIMS Complementary Platform</p>
              </div>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              This platform complements the existing college SIMS to digitize academic and manually
              maintained support workflows. Core academic modules are isolated for planned phases.
            </p>
          </div>
        </div>

        {/* V1 Foundation Notice Card */}
        <div className="bg-slate-100 rounded-xl p-6 border border-slate-200">
          <div className="flex items-start gap-3">
            <FileText className="w-5 h-5 text-slate-600 mt-0.5 flex-shrink-0" />
            <div>
              <h4 className="text-sm font-semibold text-slate-900">
                V1 Scope: Authentication & Identity Management
              </h4>
              <p className="mt-1 text-xs text-slate-600 leading-relaxed">
                You are currently running the V1 release. Core academic modules (Marks, Attendance,
                Timetable, Counsellor modules, Risk Prediction) are scheduled for subsequent phases as
                per architectural specifications.
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};
