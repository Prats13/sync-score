import { apiFetch } from "./client"
import type {
  TokenPair,
  SignupTokenResponse,
  MeResponse,
  EmailStartRequest,
  EmailVerifyOtpRequest,
  SignupCompleteRequest,
  LoginRequest,
  GoogleAuthRequest,
} from "@/lib/types"

export const authApi = {
  emailStart: (data: EmailStartRequest) =>
    apiFetch<void>("/api/v1/auth/signup/email/start", {
      method: "POST",
      body: JSON.stringify(data),
      noAuth: true,
    }),

  emailVerifyOtp: (data: EmailVerifyOtpRequest) =>
    apiFetch<SignupTokenResponse>("/api/v1/auth/signup/email/verify-otp", {
      method: "POST",
      body: JSON.stringify(data),
      noAuth: true,
    }),

  signupComplete: (data: SignupCompleteRequest, signupToken: string) =>
    apiFetch<TokenPair>("/api/v1/auth/signup/complete", {
      method: "POST",
      body: JSON.stringify(data),
      signupToken,
    }),

  signupGoogle: (data: GoogleAuthRequest) =>
    apiFetch<SignupTokenResponse | TokenPair>("/api/v1/auth/signup/google", {
      method: "POST",
      body: JSON.stringify(data),
      noAuth: true,
    }),

  login: (data: LoginRequest) =>
    apiFetch<TokenPair>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
      noAuth: true,
    }),

  loginGoogle: (data: GoogleAuthRequest) =>
    apiFetch<TokenPair>("/api/v1/auth/login/google", {
      method: "POST",
      body: JSON.stringify(data),
      noAuth: true,
    }),

  logout: () =>
    apiFetch<void>("/api/v1/auth/logout", { method: "POST" }),

  refresh: (refreshToken: string) =>
    apiFetch<TokenPair>("/api/v1/auth/token/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
      noAuth: true,
    }),

  me: () => apiFetch<MeResponse>("/api/v1/me"),
}
