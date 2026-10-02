import React, { useEffect, useState, useCallback } from 'react';
import type { UserSummary } from '../types/auth.types';
import { authApi } from '../api/auth.api';
import { AuthContext } from './auth-context-def';
import type { AuthContextType } from './auth-context-def';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const refreshUser = useCallback(async () => {
    try {
      const userData = await authApi.getCurrentUser();
      setUser(userData);
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let mounted = true;

    async function initAuth() {
      try {
        const userData = await authApi.getCurrentUser();
        if (mounted) {
          setUser(userData);
        }
      } catch {
        if (mounted) {
          setUser(null);
        }
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    }

    initAuth();

    return () => {
      mounted = false;
    };
  }, []);

  const login = async (identifier: string, password: string): Promise<void> => {
    setIsLoading(true);
    try {
      const response = await authApi.login({ identifier, password });
      setUser(response.user);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    setIsLoading(true);
    try {
      await authApi.logout();
    } finally {
      setUser(null);
      setIsLoading(false);
    }
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    logout,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
