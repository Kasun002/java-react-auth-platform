import api from "../lib/axios";
import type { ApiResponse, LoginResponse } from "../types/auth";

export const adLogin = (idToken: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/ad/login", { idToken });

export const localLogin = (email: string, password: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/login", { email, password });

export const logoutApi = (refreshToken?: string) =>
  api.post("/auth/logout", { refreshToken });
