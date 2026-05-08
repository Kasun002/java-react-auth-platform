import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { verifyOtp, resendOtp } from "../../services/authService";
import Button from "../ui/button/Button";

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60; // seconds

export default function OtpVerifyForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email") ?? "";

  const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [resendMsg, setResendMsg] = useState<string | null>(null);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // focus first box on mount
  useEffect(() => {
    inputRefs.current[0]?.focus();
  }, []);

  // countdown timer for resend
  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(id);
  }, [cooldown]);

  function handleChange(index: number, value: string) {
    // accept only digits
    const digit = value.replace(/\D/g, "").slice(-1);
    const next = [...digits];
    next[index] = digit;
    setDigits(next);
    setError(null);

    if (digit && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  }

  function handleKeyDown(index: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Backspace") {
      if (digits[index]) {
        const next = [...digits];
        next[index] = "";
        setDigits(next);
      } else if (index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
    } else if (e.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (e.key === "ArrowRight" && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  }

  function handlePaste(e: React.ClipboardEvent) {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
    if (!pasted) return;
    const next = [...digits];
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    setDigits(next);
    const focusIndex = Math.min(pasted.length, OTP_LENGTH - 1);
    inputRefs.current[focusIndex]?.focus();
  }

  const otpValue = digits.join("");
  const isComplete = otpValue.length === OTP_LENGTH;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isComplete) {
      setError("Please enter all 6 digits.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await verifyOtp(email, otpValue);
      navigate("/signin?verified=true");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "Invalid or expired OTP. Please try again.";
      setError(message);
      // clear boxes on failure so user can re-enter
      setDigits(Array(OTP_LENGTH).fill(""));
      inputRefs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    if (cooldown > 0 || resending) return;
    setError(null);
    setResendMsg(null);
    setResending(true);
    try {
      await resendOtp(email);
      setResendMsg("A new OTP has been sent to your email.");
      setCooldown(RESEND_COOLDOWN);
      setDigits(Array(OTP_LENGTH).fill(""));
      inputRefs.current[0]?.focus();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "Could not resend OTP. Please try again later.";
      setError(message);
    } finally {
      setResending(false);
    }
  }

  if (!email) {
    return (
      <div className="flex flex-col flex-1">
        <div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
          <div className="rounded-lg bg-error-50 border border-error-200 px-4 py-4 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
            <p className="font-medium mb-1">Invalid link</p>
            <p>This page requires an email address. Please register first.</p>
            <Link to="/signup" className="mt-3 inline-block font-medium underline">
              Go to Sign Up
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
        {/* Header */}
        <div className="mb-5 sm:mb-8">
          <h1 className="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
            Verify Your Email
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            We sent a 6-digit code to{" "}
            <span className="font-medium text-gray-700 dark:text-gray-300">{email}</span>.
            Enter it below to activate your account.
          </p>
        </div>

        {/* Success/Error banners */}
        {resendMsg && (
          <div className="mb-4 rounded-lg bg-success-50 border border-success-200 px-4 py-3 text-sm text-success-700 dark:bg-success-500/10 dark:border-success-500/20 dark:text-success-400">
            {resendMsg}
          </div>
        )}
        {error && (
          <div className="mb-4 rounded-lg bg-error-50 border border-error-200 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* OTP digit boxes */}
          <div className="flex items-center justify-between gap-2 sm:gap-3 mb-8">
            {digits.map((digit, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                onPaste={handlePaste}
                className={`
                  h-12 w-12 sm:h-14 sm:w-14 rounded-lg border text-center text-xl font-semibold
                  text-gray-800 dark:text-white/90 bg-transparent
                  transition-colors duration-150 outline-none
                  ${digit
                    ? "border-brand-500 ring-2 ring-brand-500/20"
                    : "border-gray-300 dark:border-gray-700"}
                  focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20
                  dark:focus:border-brand-800
                `}
                aria-label={`OTP digit ${i + 1}`}
              />
            ))}
          </div>

          <Button
            className="w-full"
            size="sm"
            disabled={loading || !isComplete}
          >
            {loading ? "Verifying..." : "Verify Email"}
          </Button>
        </form>

        {/* Resend + navigation links */}
        <div className="mt-6 space-y-2">
          <p className="text-sm text-center text-gray-500 dark:text-gray-400 sm:text-start">
            Didn&apos;t receive the code?{" "}
            {cooldown > 0 ? (
              <span className="text-gray-400 dark:text-gray-600">
                Resend in {cooldown}s
              </span>
            ) : (
              <button
                type="button"
                onClick={handleResend}
                disabled={resending}
                className="font-medium text-brand-500 hover:text-brand-600 dark:text-brand-400 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {resending ? "Sending..." : "Resend OTP"}
              </button>
            )}
          </p>
          <p className="text-sm text-center text-gray-700 dark:text-gray-400 sm:text-start">
            Already verified?{" "}
            <Link
              to="/signin"
              className="text-brand-500 hover:text-brand-600 dark:text-brand-400"
            >
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
