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
  HeartHandshake,
  Building,
  Layers,
  Sparkles,
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

  const isActive = (path: string, search?: string) => {
    if (search) {
      return location.pathname === path && location.search === search;
    }
    return location.pathname === path && (!location.search || location.search === '');
  };

  if (!user) return null;

  const isClassTeacher =
    user.role === 'CLASS_TEACHER' ||
    user.responsibilities?.some(
      (r) => r.type === 'CLASS_TEACHER' && r.status === 'ACTIVE'
    );

  const isCounsellor =
    user.role === 'COUNSELLOR' ||
    user.responsibilities?.some(
      (r) => r.type === 'COUNSELLOR' && r.status === 'ACTIVE'
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

          {/* Role-Specific Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            {/* STUDENT NAVIGATION */}
            {user.role === 'STUDENT' && (
              <>
                <Link
                  to="/"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <LayoutDashboard className="w-3.5 h-3.5" />
                  My Academics
                </Link>

                <Link
                  to="/timetable"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/timetable')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  My Timetable
                </Link>

                <Link
                  to="/?tab=counselling"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/', '?tab=counselling')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <HeartHandshake className="w-3.5 h-3.5" />
                  My Counselling
                </Link>
              </>
            )}

            {/* FACULTY NAVIGATION */}
            {user.role === 'FACULTY' && (
              <>
                <Link
                  to="/"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <BookOpen className="w-3.5 h-3.5" />
                  My Subjects & Classes
                </Link>

                <Link
                  to="/timetable"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/timetable')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  My Timetable
                </Link>

                {/* Class Teacher Responsibility Navigation */}
                {isClassTeacher && (
                  <Link
                    to="/students"
                    className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/students')
                        ? 'bg-teal-50 text-teal-800 font-bold'
                        : 'text-teal-700 hover:bg-teal-50'
                    }`}
                  >
                    <Users className="w-3.5 h-3.5" />
                    My Section Students
                  </Link>
                )}

                {/* Counsellor Responsibility Navigation */}
                {isCounsellor && (
                  <Link
                    to="/?tab=counselling"
                    className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/', '?tab=counselling')
                        ? 'bg-indigo-50 text-indigo-700 font-bold'
                        : 'text-indigo-700 hover:bg-indigo-50'
                    }`}
                  >
                    <HeartHandshake className="w-3.5 h-3.5" />
                    My Counselling
                  </Link>
                )}
              </>
            )}

            {/* HOD NAVIGATION (Strictly HOD's Department, hierarchical) */}
            {user.role === 'HOD' && (
              <>
                <Link
                  to="/"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Layers className="w-3.5 h-3.5" />
                  My Department
                </Link>

                <Link
                  to="/timetable"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/timetable')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  Timetable
                </Link>

                <Link
                  to="/users"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/users')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  Dept. Users
                </Link>
              </>
            )}

            {/* PRINCIPAL NAVIGATION (College-wide, nested departments & programs) */}
            {user.role === 'PRINCIPAL' && (
              <>
                <Link
                  to="/"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Building className="w-3.5 h-3.5" />
                  My College
                </Link>

                <Link
                  to="/students"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/students')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  College Students
                </Link>

                <Link
                  to="/timetable"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/timetable')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  Timetable
                </Link>

                <Link
                  to="/users"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/users')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  Institutional Users
                </Link>
              </>
            )}

            {/* ADMIN NAVIGATION */}
            {user.role === 'ADMIN' && (
              <>
                <Link
                  to="/"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <LayoutDashboard className="w-3.5 h-3.5" />
                  Dashboard
                </Link>

                <Link
                  to="/users"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/users')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Users className="w-3.5 h-3.5" />
                  User Management
                </Link>

                <Link
                  to="/students"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/students')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <BookOpen className="w-3.5 h-3.5" />
                  Student Directory
                </Link>

                <Link
                  to="/timetable"
                  className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                    isActive('/timetable')
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                  }`}
                >
                  <Calendar className="w-3.5 h-3.5" />
                  Timetable
                </Link>
              </>
            )}

            {/* Profile Link for everyone */}
            <Link
              to="/profile"
              className={`flex items-center gap-1.5 px-3 py-2 rounded-md text-xs font-semibold transition-colors ${
                isActive('/profile')
                  ? 'bg-indigo-50 text-indigo-700 font-bold'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
              }`}
            >
              <UserIcon className="w-3.5 h-3.5" />
              Profile
            </Link>
          </nav>

          {/* User info & Logout */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <div className="text-xs font-bold text-slate-800 leading-tight">
                  {user.fullName}
                </div>
                <div className="text-[11px] text-slate-500 font-mono">
                  {user.registrationNumber || user.uniqueId || user.username}
                </div>
              </div>
              <div className="flex flex-col items-end gap-1">
                <span
                  className={`px-2 py-0.5 rounded-full text-[11px] font-bold border ${
                    roleBadgeStyles[user.role] ||
                    'bg-slate-100 text-slate-700 border-slate-200'
                  }`}
                >
                  {user.role}
                </span>
                {/* Specific active responsibilities tags */}
                {isClassTeacher && user.role === 'FACULTY' && (
                  <span className="text-[10px] px-1.5 py-0.2 rounded bg-teal-50 text-teal-700 border border-teal-200 font-semibold">
                    Class Teacher
                  </span>
                )}
                {isCounsellor && user.role === 'FACULTY' && (
                  <span className="text-[10px] px-1.5 py-0.2 rounded bg-indigo-50 text-indigo-700 border border-indigo-200 font-semibold">
                    Counsellor
                  </span>
                )}
              </div>
            </div>

            <button
              onClick={handleLogout}
              title="Sign Out"
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-md border border-slate-200 hover:border-red-200 transition-colors cursor-pointer"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
