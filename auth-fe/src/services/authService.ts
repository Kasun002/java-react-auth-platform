import axios from "axios";
import api from "../lib/axios";
import type { ApiResponse, LoginResponse, RegisterRequest } from "../types/auth";

/**
 * Exchange a refresh token for a new token pair.
 * Uses raw axios — NOT the intercepted `api` instance — to avoid refresh loops.
 */
export const refreshAccessToken = (refreshToken: string) =>
  axios.post<ApiResponse<LoginResponse>>(
    `${import.meta.env.VITE_API_BASE_URL as string}/auth/refresh`,
    { refreshToken },
    { headers: { "Content-Type": "application/json" } }
  );

export const register = (data: RegisterRequest) =>
  api.post<ApiResponse<void>>("/auth/register", data);

export const adLogin = (idToken: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/ad/login", { idToken });

export const localLogin = (email: string, password: string) =>
  api.post<ApiResponse<LoginResponse>>("/auth/login", { email, password });

export const logoutApi = (refreshToken?: string) =>
  api.post("/auth/logout", { refreshToken });

export const forgotPassword = (email: string) =>
  api.post<ApiResponse<void>>("/auth/forgot-password", { email });

export const resetPassword = (token: string, newPassword: string) =>
  api.post<ApiResponse<void>>("/auth/reset-password", { token, newPassword });

export const verifyOtp = (email: string, otp: string) =>
  api.post<ApiResponse<void>>("/auth/verify-otp", { email, otp });

export const resendOtp = (email: string) =>
  api.post<ApiResponse<void>>("/auth/resend-otp", { email });
