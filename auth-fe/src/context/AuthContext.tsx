import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import {
  setAuthTokens,
  setOnSessionExpired,
  setOnTokenRefreshed,
} from "../lib/axios";
import { refreshAccessToken, logoutApi } from "../services/authService";
import type { UserDto } from "../types/auth";

// ── Public API ────────────────────────────────────────────────────────────────

interface AuthContextValue {
  /** In-memory access token — never written to localStorage. */
  accessToken: string | null;
  user: UserDto | null;
  isAuthenticated: boolean;
  /** True while a silent token refresh is in flight on first load. */
  rehydrating: boolean;
  login: (
    accessToken: string,
    refreshToken: string,
    user: UserDto,
    remember: boolean
  ) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// ── Storage helpers ───────────────────────────────────────────────────────────

function getStoredSession(): {
  refreshToken: string | null;
  user: UserDto | null;
  remember: boolean;
} {
  try {
    // localStorage → "keep me logged in" (persists across browser sessions)
    const lsToken = localStorage.getItem("refreshToken");
    if (lsToken) {
      return {
        refreshToken: lsToken,
        user: JSON.parse(localStorage.getItem("user") ?? "null") as UserDto | null,
        remember: true,
      };
    }
    // sessionStorage → session-only (cleared when browser tab closes)
    const ssToken = sessionStorage.getItem("refreshToken");
    if (ssToken) {
      return {
        refreshToken: ssToken,
        user: JSON.parse(sessionStorage.getItem("user") ?? "null") as UserDto | null,
        remember: false,
      };
    }
  } catch {
    // corrupted storage — treat as logged out
  }
  return { refreshToken: null, user: null, remember: false };
}

function saveSession(refreshToken: string, user: UserDto, remember: boolean) {
  const primary = remember ? localStorage : sessionStorage;
  const secondary = remember ? sessionStorage : localStorage;
  primary.setItem("refreshToken", refreshToken);
  primary.setItem("user", JSON.stringify(user));
  // Clear from the other store so there are never two valid sessions
  secondary.removeItem("refreshToken");
  secondary.removeItem("user");
}

function clearSession() {
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
  sessionStorage.removeItem("refreshToken");
  sessionStorage.removeItem("user");
}

// ── Provider ──────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [user, setUser] = useState<UserDto | null>(null);
  // true while we attempt to restore the session on first paint
  const [rehydrating, setRehydrating] = useState(true);

  // ── Session restoration on mount ─────────────────────────────────────────
  useEffect(() => {
    const { refreshToken, user: storedUser, remember } = getStoredSession();

    if (!refreshToken) {
      setRehydrating(false);
      return;
    }

    // Use the stored user immediately so the UI can render with data while refreshing
    setUser(storedUser);

    refreshAccessToken(refreshToken)
      .then((res) => {
        const data = res.data.data;
        if (!data) throw new Error("empty refresh response");
        setAccessToken(data.accessToken);
        setUser(data.user);
        setAuthTokens(data.accessToken, data.refreshToken);
        saveSession(data.refreshToken, data.user, remember);
      })
      .catch(() => {
        // Refresh token expired or invalid — force sign-in
        clearSession();
        setAccessToken(null);
        setUser(null);
        setAuthTokens(null, null);
      })
      .finally(() => setRehydrating(false));
  // Intentionally empty deps — runs exactly once on mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Register axios callbacks once ─────────────────────────────────────────
  useEffect(() => {
    setOnTokenRefreshed((newAccess, newRefresh) => {
      setAccessToken(newAccess);
      // Persist the rotated refresh token in whichever store is currently active
      const { remember } = getStoredSession();
      const { user: storedUser } = getStoredSession();
      if (storedUser) saveSession(newRefresh, storedUser, remember);
    });

    setOnSessionExpired(() => {
      clearSession();
      setAccessToken(null);
      setUser(null);
      setAuthTokens(null, null);
      globalThis.location.href = "/signin";
    });
  }, []);

  // ── Login ─────────────────────────────────────────────────────────────────
  const login = useCallback(
    (
      newAccessToken: string,
      newRefreshToken: string,
      newUser: UserDto,
      remember: boolean
    ) => {
      setAccessToken(newAccessToken);
      setUser(newUser);
      setAuthTokens(newAccessToken, newRefreshToken);
      saveSession(newRefreshToken, newUser, remember);
    },
    []
  );

  // ── Logout ────────────────────────────────────────────────────────────────
  const logout = useCallback(async () => {
    const { refreshToken } = getStoredSession();
    try {
      await logoutApi(refreshToken ?? undefined);
    } catch {
      // best-effort — clear client state regardless
    }
    clearSession();
    setAccessToken(null);
    setUser(null);
    setAuthTokens(null, null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        accessToken,
        user,
        isAuthenticated: !!accessToken,
        rehydrating,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
