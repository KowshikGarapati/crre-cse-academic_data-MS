import { createContext } from 'react';
import type { UserSummary } from '../types/auth.types';

export interface AuthContextType {
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
