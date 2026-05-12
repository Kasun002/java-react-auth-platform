import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useAuth } from "../../context/AuthContext";
import { EyeCloseIcon, EyeIcon } from "../../icons";
import { localLogin } from "../../services/authService";
import { generateCodeChallenge, generateCodeVerifier } from "../../utils/pkce";
import Label from "../form/Label";
import Checkbox from "../form/input/Checkbox";
import Input from "../form/input/InputField";
import Button from "../ui/button/Button";

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL;
const REALM = import.meta.env.VITE_KEYCLOAK_REALM;
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;

const AD_ERROR_MESSAGES: Record<string, string> = {
  ad_failed: "AD sign-in failed. Please try again.",
  ad_cancelled: "AD sign-in was cancelled.",
  ad_no_code: "AD sign-in did not return a code. Please try again.",
  ad_no_verifier: "Session expired. Please try again.",
};

export default function SignInForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isChecked, setIsChecked] = useState(false);

  const [error, setError] = useState<string | null>(
    AD_ERROR_MESSAGES[searchParams.get("error") ?? ""] ?? null
  );

  let success;
  if (searchParams.get("verified") === "true") {
    success = "Email verified! Your account is active. Please sign in.";
  } else if (searchParams.get("reset") === "true") {
    success =
      '"Password reset successfully. Please sign in with your new password."';
  }
  const [loading, setLoading] = useState(false);
  const [adLoading, setAdLoading] = useState(false);

  async function handleLocalLogin(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const response = await localLogin(email, password);
      const { data } = response.data;
      if (!data) throw new Error(response.data.message);
      login(data.accessToken, data.refreshToken, data.user);
      navigate("/");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "Invalid email or password.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  async function handleAdLogin() {
    setError(null);
    setAdLoading(true);
    try {
      const verifier = generateCodeVerifier();
      const challenge = await generateCodeChallenge(verifier);
      sessionStorage.setItem("pkce_verifier", verifier);

      const redirectUri = `${globalThis.location.origin}/auth/callback`;
      const params = new URLSearchParams({
        response_type: "code",
        client_id: CLIENT_ID,
        redirect_uri: redirectUri,
        scope: "openid email profile",
        code_challenge: challenge,
        code_challenge_method: "S256",
      });

      globalThis.location.href = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/auth?${params}`;
    } catch {
      setError("Could not initiate AD sign-in. Please try again.");
      setAdLoading(false);
    }
  }

  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
        <div>
          <div className="mb-5 sm:mb-8">
            <h1 className="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
              Sign In
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Enter your email and password to sign in!
            </p>
          </div>

          {success && (
            <div className="mb-4 rounded-lg bg-success-50 border border-success-200 px-4 py-3 text-sm text-success-700 dark:bg-success-500/10 dark:border-success-500/20 dark:text-success-400">
              {success}
            </div>
          )}
          {error && (
            <div className="mb-4 rounded-lg bg-error-50 border border-error-200 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
              {error}
            </div>
          )}

          <div>
            <div className="grid grid-cols-1 gap-3">
              {/* AD Login */}
              <button
                type="button"
                onClick={handleAdLogin}
                disabled={adLoading}
                className="inline-flex items-center justify-center gap-3 py-3 text-sm font-normal text-gray-700 transition-colors bg-gray-100 rounded-lg px-7 hover:bg-gray-200 hover:text-gray-800 dark:bg-white/5 dark:text-white/90 dark:hover:bg-white/10 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {adLoading ? (
                  <span className="inline-block w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
                ) : (
                  /* Keycloak logo mark */
                  <img
                    src="/images/icons/file-image-keycloak.svg"
                    className="h-5"
                    alt="Keycloak"
                  />
                )}
                Sign in with Keycloak
              </button>
            </div>

            <div className="relative py-3 sm:py-5">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200 dark:border-gray-800" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="p-2 text-gray-400 bg-white dark:bg-gray-900 sm:px-5 sm:py-2">
                  Or
                </span>
              </div>
            </div>

            <form onSubmit={handleLocalLogin}>
              <div className="space-y-6">
                <div>
                  <Label>
                    Email <span className="text-error-500">*</span>
                  </Label>
                  <Input
                    type="email"
                    placeholder="info@gmail.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <Label>
                    Password <span className="text-error-500">*</span>
                  </Label>
                  <div className="relative">
                    <Input
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                    />
                    <button
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                    >
                      {showPassword ? (
                        <EyeIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                      ) : (
                        <EyeCloseIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                      )}
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Checkbox checked={isChecked} onChange={setIsChecked} />
                    <span className="block font-normal text-gray-700 text-theme-sm dark:text-gray-400">
                      Keep me logged in
                    </span>
                  </div>
                  <Link
                    to="/forgot-password"
                    className="text-sm text-brand-500 hover:text-brand-600 dark:text-brand-400"
                  >
                    Forgot password?
                  </Link>
                </div>

                <div>
                  <Button className="w-full" size="sm" disabled={loading}>
                    {loading ? "Signing in..." : "Sign in"}
                  </Button>
                </div>
              </div>
            </form>

            <div className="mt-5">
              <p className="text-sm font-normal text-center text-gray-700 dark:text-gray-400 sm:text-start">
                Don&apos;t have an account?{" "}
                <Link
                  to="/signup"
                  className="text-brand-500 hover:text-brand-600 dark:text-brand-400"
                >
                  Sign Up
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
