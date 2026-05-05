import { createContext, useContext, useState, useEffect, useCallback, useRef, type ReactNode } from 'react';
import type { AuthResponse } from '../types';
import toast from 'react-hot-toast';

// ==================== CONFIGURATION ====================
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000;  // 30 minutes of inactivity → auto-logout
const TOKEN_CHECK_INTERVAL_MS = 60 * 1000;      // Check token expiry every 60 seconds
const WARNING_BEFORE_EXPIRY_MS = 5 * 60 * 1000;  // Show warning 5 minutes before expiry

interface AuthContextType {
  user: AuthResponse | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (authResponse: AuthResponse) => void;
  updateUser: (userData: Partial<AuthResponse>) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Decode JWT payload to extract expiration time.
 * Returns expiry timestamp in ms, or null if invalid.
 */
function getTokenExpiry(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp ? payload.exp * 1000 : null; // Convert seconds → ms
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const inactivityTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const warningShown = useRef(false);

  // ==================== LOGOUT ====================
  const logout = useCallback((reason?: string) => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('loginTimestamp');
    warningShown.current = false;

    if (reason) {
      toast.error(reason, { duration: 5000 });
      // Small delay to let the toast render before redirect
      setTimeout(() => {
        window.location.href = '/login';
      }, 500);
    }
  }, []);

  // ==================== INACTIVITY TRACKING ====================
  const resetInactivityTimer = useCallback(() => {
    if (inactivityTimer.current) {
      clearTimeout(inactivityTimer.current);
    }

    const currentToken = localStorage.getItem('token');
    if (!currentToken) return;

    inactivityTimer.current = setTimeout(() => {
      logout('Session expired due to inactivity. Please log in again.');
    }, INACTIVITY_TIMEOUT_MS);
  }, [logout]);

  // ==================== TOKEN EXPIRY CHECK ====================
  useEffect(() => {
    if (!token) return;

    const checkTokenExpiry = () => {
      const expiry = getTokenExpiry(token);
      if (!expiry) return;

      const now = Date.now();
      const timeLeft = expiry - now;

      if (timeLeft <= 0) {
        // Token has expired
        logout('Your session has expired. Please log in again.');
      } else if (timeLeft <= WARNING_BEFORE_EXPIRY_MS && !warningShown.current) {
        // Show warning before expiry
        warningShown.current = true;
        const minutesLeft = Math.ceil(timeLeft / 60000);
        toast(`Your session will expire in ${minutesLeft} minute${minutesLeft > 1 ? 's' : ''}. Please save your work.`, {
          icon: '⚠️',
          duration: 10000,
        });
      }
    };

    // Check immediately
    checkTokenExpiry();

    // Then check periodically
    const interval = setInterval(checkTokenExpiry, TOKEN_CHECK_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [token, logout]);

  // ==================== ACTIVITY LISTENERS ====================
  useEffect(() => {
    if (!token) return;

    const activityEvents = ['mousedown', 'keydown', 'scroll', 'touchstart'];

    const handleActivity = () => resetInactivityTimer();

    activityEvents.forEach(event =>
      window.addEventListener(event, handleActivity, { passive: true })
    );

    // Start the initial timer
    resetInactivityTimer();

    return () => {
      activityEvents.forEach(event =>
        window.removeEventListener(event, handleActivity)
      );
      if (inactivityTimer.current) {
        clearTimeout(inactivityTimer.current);
      }
    };
  }, [token, resetInactivityTimer]);

  // ==================== RESTORE SESSION ====================
  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');

    if (savedToken && savedUser) {
      // Check if token is already expired before restoring
      const expiry = getTokenExpiry(savedToken);
      if (expiry && expiry <= Date.now()) {
        // Token already expired — clean up
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('loginTimestamp');
        return;
      }

      setToken(savedToken);
      setUser(JSON.parse(savedUser));
    }
  }, []);

  // ==================== LOGIN ====================
  const login = (authResponse: AuthResponse) => {
    setUser(authResponse);
    setToken(authResponse.token);
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('user', JSON.stringify(authResponse));
    localStorage.setItem('loginTimestamp', Date.now().toString());
    warningShown.current = false;
  };

  // ==================== UPDATE USER ====================
  const updateUser = (userData: Partial<AuthResponse>) => {
    if (user) {
      const updatedUser = { ...user, ...userData };
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
  };

  const isAuthenticated = !!token;
  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, isAdmin, login, updateUser, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
