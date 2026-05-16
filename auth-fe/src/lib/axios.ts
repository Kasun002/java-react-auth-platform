import axios from "axios";

// ── In-memory auth slots (set by AuthContext — never touches localStorage) ────
let _accessToken: string | null = null;
let _refreshToken: string | null = null;
let _onTokenRefreshed: ((access: string, refresh: string) => void) | null = null;
let _onSessionExpired: (() => void) | null = null;

export function setAuthTokens(access: string | null, refresh: string | null): void {
  _accessToken = access;
  _refreshToken = refresh;
}

export function setOnTokenRefreshed(
  fn: (access: string, refresh: string) => void
): void {
  _onTokenRefreshed = fn;
}

export function setOnSessionExpired(fn: () => void): void {
  _onSessionExpired = fn;
}

// ── Axios instance ────────────────────────────────────────────────────────────
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach the in-memory access token on every outbound request
api.interceptors.request.use((config) => {
  if (_accessToken) {
    config.headers.Authorization = `Bearer ${_accessToken}`;
  }
  return config;
});

// ── Refresh coordination ──────────────────────────────────────────────────────
let isRefreshing = false;
const waitingQueue: Array<(token: string) => void> = [];

function drainQueue(newToken: string) {
  waitingQueue.splice(0).forEach((cb) => cb(newToken));
}

// On 401: attempt one silent token refresh then replay the failed request.
// Uses raw axios (not the intercepted instance) to avoid an infinite loop.
api.interceptors.response.use(
  (res) => res,
  async (error: {
    response?: { status: number };
    config: { headers: Record<string, string>; _retry?: boolean };
  }) => {
    const original = error.config;

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error);
    }

    if (!_refreshToken) {
      _onSessionExpired?.();
      return Promise.reject(error);
    }

    // Another request is already refreshing — queue this one
    if (isRefreshing) {
      return new Promise((resolve) => {
        waitingQueue.push((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          resolve(api(original));
        });
      });
    }

    original._retry = true;
    isRefreshing = true;

    try {
      const res = await axios.post<{
        data: { accessToken: string; refreshToken: string } | null;
      }>(
        `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
        { refreshToken: _refreshToken },
        { headers: { "Content-Type": "application/json" } }
      );

      const refreshed = res.data.data;
      if (!refreshed) throw new Error("Empty refresh response");

      _accessToken = refreshed.accessToken;
      _refreshToken = refreshed.refreshToken;

      // Notify AuthContext so it can sync React state and storage
      _onTokenRefreshed?.(refreshed.accessToken, refreshed.refreshToken);

      original.headers.Authorization = `Bearer ${refreshed.accessToken}`;
      drainQueue(refreshed.accessToken);
      return api(original);
    } catch {
      waitingQueue.length = 0;
      _onSessionExpired?.();
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  }
);

export default api;
