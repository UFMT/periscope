import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import * as authApi from '../api/auth';
import {
  clearSession,
  getStoredToken,
  setStoredToken,
  setUnauthorizedHandler,
} from '../api/client';
import type { User } from '../types';
import { AuthContext, type AuthContextValue } from './authContext';

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const handleUnauthorized = useCallback(() => {
    clearSession();
    setToken(null);
    setUser(null);
    setLoading(false);
    navigate('/login', { replace: true });
  }, [navigate]);

  useEffect(() => {
    setUnauthorizedHandler(handleUnauthorized);
    return () => setUnauthorizedHandler(null);
  }, [handleUnauthorized]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const me = await authApi.me(true);
        if (!cancelled) {
          setUser(me);
        }
      } catch {
        if (!cancelled) {
          clearSession();
          setToken(null);
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login({ username, password });
    setStoredToken(result.token);
    setLoading(false);
    setToken(result.token);
    setUser(result.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // discard session locally regardless of server response
    } finally {
      clearSession();
      setToken(null);
      setUser(null);
      setLoading(false);
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const refreshUser = useCallback(async () => {
    const me = await authApi.me();
    setUser(me);
  }, []);

  const updateCurrentUser = useCallback((nextUser: User) => {
    setUser(nextUser);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: !!user,
      isAdmin: user?.userLevel === 'ADMIN',
      loading,
      login,
      logout,
      refreshUser,
      updateCurrentUser,
    }),
    [user, token, loading, login, logout, refreshUser, updateCurrentUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
