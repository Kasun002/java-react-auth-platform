import api from "../lib/axios";
import type { ApiResponse, LoginResponse, RegisterRequest } from "../types/auth";

export const register = (data: RegisterRequest) =>
  api.post<ApiResponse<void>>("/auth/register", data);

export const adLogin = (idToken: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/ad/login", { idToken });

export const localLogin = (email: string, password: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/login", { email, password });

export const logoutApi = (refreshToken?: string) =>
  api.post("/auth/logout", { refreshToken });
