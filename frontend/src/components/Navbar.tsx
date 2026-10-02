import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import {
  GraduationCap,
  LogOut,
  User as UserIcon,
  LayoutDashboard,
  Users,
  Calendar,
  BookOpen,
} from 'lucide-react';
import type { Role } from '../types/auth.types';

const roleBadgeStyles: Record<Role, string> = {
  ADMIN: 'bg-purple-100 text-purple-700 border-purple-200',
  PRINCIPAL: 'bg-rose-100 text-rose-700 border-rose-200',
  HOD: 'bg-amber-100 text-amber-700 border-amber-200',
  FACULTY: 'bg-blue-100 text-blue-700 border-blue-200',
  CLASS_TEACHER: 'bg-teal-100 text-teal-700 border-teal-200',
  COUNSELLOR: 'bg-indigo-100 text-indigo-700 border-indigo-200',
  STUDENT: 'bg-emerald-100 text-emerald-700 border-emerald-200',
};

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  // Determine role-based access
  const canManageUsers =
    user?.role === 'ADMIN' || user?.role === 'PRINCIPAL' || user?.role === 'HOD';

  const isClassTeacher =
    user?.role === 'CLASS_TEACHER' ||
    user?.responsibilities?.some(
      (r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE'
    );

  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Logo & College DMS branding */}
          <div className="flex items-center gap-3">
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="w-10 h-10 rounded-lg bg-indigo-600 flex items-center justify-center text-white shadow-sm transition-transform group-hover:scale-105">
                <GraduationCap className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <span className="text-base font-bold text-slate-900 tracking-tight leading-tight">
                    CRRE Academic DMS
                  </span>
                  <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-700 border border-indigo-200">
                    V2 CSE AI&ML
                  </span>
                </div>
                <span className="text-xs text-slate-500 font-medium block">
                  Digitalization & Support Platform
                </span>
              </div>
            </Link>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            <Link
              to="/"
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                isActive('/')
                  ? 'bg-indigo-50 text-indigo-700 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              Dashboard
            </Link>

            {canManageUsers && (
              <Link
                to="/users"
                className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive('/users')
                    ? 'bg-indigo-50 text-indigo-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Users className="w-4 h-4" />
                User & Roles
              </Link>
            )}

            <Link
              to="/students"
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                isActive('/students')
                  ? 'bg-indigo-50 text-indigo-700 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <BookOpen className="w-4 h-4" />
              Students (66)
            </Link>

            <Link
              to="/timetable"
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                isActive('/timetable')
                  ? 'bg-indigo-50 text-indigo-700 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <Calendar className="w-4 h-4" />
              Timetable
            </Link>

            <Link
              to="/profile"
              className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                isActive('/profile')
                  ? 'bg-indigo-50 text-indigo-700 font-semibold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <UserIcon className="w-4 h-4" />
              Profile
            </Link>
          </nav>

          {/* User info & Logout */}
          <div className="flex items-center gap-4">
            {user && (
              <div className="flex items-center gap-3">
                <div className="text-right hidden sm:block">
                  <div className="text-sm font-semibold text-slate-800 leading-tight">
                    {user.fullName}
                  </div>
                  <div className="text-xs text-slate-500 font-mono">
                    {user.registrationNumber || user.uniqueId || user.username}
                  </div>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-xs font-semibold border ${
                      roleBadgeStyles[user.role] ||
                      'bg-slate-100 text-slate-700 border-slate-200'
                    }`}
                  >
                    {user.role}
                  </span>
                  {isClassTeacher && user.role !== 'CLASS_TEACHER' && (
                    <span className="text-[10px] px-1.5 py-0.2 rounded bg-teal-50 text-teal-700 border border-teal-200 font-medium">
                      Class Teacher (Sec 1)
                    </span>
                  )}
                </div>
              </div>
            )}

            <button
              onClick={handleLogout}
              title="Sign Out"
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-md border border-slate-200 hover:border-red-200 transition-colors cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
