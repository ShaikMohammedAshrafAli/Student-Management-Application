import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { tokenStorage } from '../utils/tokenStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => tokenStorage.getUser());
  const [loading, setLoading] = useState(false);

  // Keep state in sync if another tab logs in/out.
  useEffect(() => {
    const onStorage = () => setUser(tokenStorage.getUser());
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const data = await authApi.login({ email, password });
      const { accessToken, refreshToken, ...rest } = data;
      tokenStorage.setSession({ accessToken, refreshToken, ...rest });
      setUser(rest);
      return rest;
    } finally {
      setLoading(false);
    }
  };

  const register = async (payload) => {
    setLoading(true);
    try {
      const data = await authApi.register(payload);
      const { accessToken, refreshToken, ...rest } = data;
      tokenStorage.setSession({ accessToken, refreshToken, ...rest });
      setUser(rest);
      return rest;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    tokenStorage.clear();
    setUser(null);
    if (refreshToken) {
      // Best-effort - the user is logged out client-side regardless of outcome.
      authApi.logout(refreshToken).catch(() => {});
    }
  };

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isAdmin: user?.role === 'ADMIN',
      isStudent: user?.role === 'STUDENT',
      loading,
      login,
      register,
      logout,
    }),
    [user, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
