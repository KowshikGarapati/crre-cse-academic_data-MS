import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { GraduationCap, Lock, User, AlertCircle, Loader2, ArrowRight } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!identifier.trim() || !password) {
      setError('Please provide your identifier and password');
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await login(identifier.trim(), password);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Login failed. Please check your credentials and try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const fillCredentials = (id: string, pass: string) => {
    setIdentifier(id);
    setPassword(pass);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex flex-col justify-center py-10 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-xl text-center">
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-600 text-white shadow-xl shadow-indigo-600/30 mb-3">
          <GraduationCap className="w-10 h-10" />
        </div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          CRRE Academic DMS
        </h2>
        <p className="mt-1 text-sm text-indigo-200">
          CSE AI&ML Academic Data & Support Digitalization Platform (V2 Prototype)
        </p>
      </div>

      <div className="mt-6 sm:mx-auto sm:w-full sm:max-w-xl px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-2xl rounded-2xl sm:px-10 border border-slate-100">
          <form className="space-y-4" onSubmit={handleSubmit}>
            <div>
              <label
                htmlFor="identifier"
                className="block text-xs font-semibold uppercase tracking-wider text-slate-700"
              >
                Identifier
              </label>
              <div className="mt-1.5 relative rounded-lg shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <User className="h-5 w-5" />
                </div>
                <input
                  id="identifier"
                  name="identifier"
                  type="text"
                  autoComplete="username"
                  required
                  placeholder="Username, Reg No (e.g. 25B81A4201), Unique ID, or Email"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border border-slate-300 rounded-lg text-slate-900 text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-xs font-semibold uppercase tracking-wider text-slate-700"
              >
                Password
              </label>
              <div className="mt-1.5 relative rounded-lg shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Lock className="h-5 w-5" />
                </div>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border border-slate-300 rounded-lg text-slate-900 text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>
            </div>

            {error && (
              <div className="rounded-lg bg-red-50 border border-red-200 p-3.5 flex items-start gap-2.5 text-red-700 text-sm">
                <AlertCircle className="w-5 h-5 flex-shrink-0 text-red-500 mt-0.5" />
                <div>{error}</div>
              </div>
            )}

            <div>
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-60 disabled:cursor-not-allowed transition-all cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Signing in...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Quick Demo Credentials */}
          <div className="mt-6 border-t border-slate-200 pt-5">
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold text-slate-600 uppercase tracking-wider">
                V2 Prototype Accounts (Demo Ready)
              </span>
              <span className="text-[11px] text-slate-400">Click to fill</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => fillCredentials('principal', 'Admin@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-rose-300 hover:bg-rose-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">Principal</div>
                  <div className="text-[11px] text-slate-500">principal / Admin@123</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-100 text-rose-700">
                  CRRE
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('hod.cse', 'Admin@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-amber-300 hover:bg-amber-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">HOD CSE</div>
                  <div className="text-[11px] text-slate-500">hod.cse / Admin@123</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-700">
                  CSE Dept
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('ramalakshmi', 'Faculty@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-teal-300 hover:bg-teal-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">Faculty & Class Teacher</div>
                  <div className="text-[11px] text-slate-500">CH. Rama Lakshmi (DMGT)</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-teal-100 text-teal-700">
                  Sec 1 CT
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('faculty.cse', 'Faculty@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-blue-300 hover:bg-blue-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">Faculty (ADS)</div>
                  <div className="text-[11px] text-slate-500">faculty.cse / Faculty@123</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-blue-100 text-blue-700">
                  Faculty
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('counsellor.cse', 'Faculty@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">Counsellor</div>
                  <div className="text-[11px] text-slate-500">counsellor.cse / Faculty@123</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-indigo-100 text-indigo-700">
                  CSE AI&ML
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('25b81a4201', 'Student@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-emerald-300 hover:bg-emerald-50/50 transition-colors text-left cursor-pointer"
              >
                <div>
                  <div className="font-semibold text-slate-800">Student (Akhil)</div>
                  <div className="text-[11px] text-slate-500">25B81A4201 / Student@123</div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-700">
                  Sec 1 Roll 1
                </span>
              </button>

              <button
                type="button"
                onClick={() => fillCredentials('admin', 'Admin@123')}
                className="flex items-center justify-between p-2.5 rounded-lg border border-slate-200 hover:border-purple-300 hover:bg-purple-50/50 transition-colors text-left sm:col-span-2 cursor-pointer"
              >
                <div>
                  <span className="font-semibold text-slate-800">System Admin</span>
                  <span className="text-slate-500 ml-2">admin / Admin@123 (Full Superuser Access)</span>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-purple-100 text-purple-700">
                  ADMIN
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
