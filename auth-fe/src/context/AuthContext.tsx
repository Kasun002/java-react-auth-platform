import {
  createContext,
  useContext,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import type { UserDto } from "../types/auth";
import { logoutApi } from "../services/authService";

interface AuthState {
  user: UserDto | null;
  accessToken: string | null;
  refreshToken: string | null;
}

interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, user: UserDto) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadFromStorage(): AuthState {
  try {
    return {
      accessToken: localStorage.getItem("accessToken"),
      refreshToken: localStorage.getItem("refreshToken"),
      user: JSON.parse(localStorage.getItem("user") ?? "null"),
    };
  } catch {
    return { accessToken: null, refreshToken: null, user: null };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadFromStorage);

  const login = useCallback(
    (accessToken: string, refreshToken: string, user: UserDto) => {
      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      localStorage.setItem("user", JSON.stringify(user));
      setState({ accessToken, refreshToken, user });
    },
    [],
  );

  const logout = useCallback(async () => {
    try {
      await logoutApi(state.refreshToken ?? undefined);
    } catch {
      // best-effort; clear client state regardless
    }
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
    setState({ accessToken: null, refreshToken: null, user: null });
  }, [state.refreshToken]);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        isAuthenticated: !!state.accessToken,
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
