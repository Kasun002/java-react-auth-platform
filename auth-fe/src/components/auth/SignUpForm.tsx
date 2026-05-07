import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { EyeCloseIcon, EyeIcon } from "../../icons";
import Label from "../form/Label";
import Input from "../form/input/InputField";
import Checkbox from "../form/input/Checkbox";
import Button from "../ui/button/Button";
import { register } from "../../services/authService";

// ── Password rules (mirrors backend StrongPasswordValidator) ──────────────────
const PASSWORD_RULES = [
  { label: "At least 12 characters", test: (p: string) => p.length >= 12 },
  { label: "One uppercase letter",   test: (p: string) => /[A-Z]/.test(p) },
  { label: "One lowercase letter",   test: (p: string) => /[a-z]/.test(p) },
  { label: "One number",             test: (p: string) => /\d/.test(p) },
  { label: "One special character",  test: (p: string) => /[!@#$%^&*(),.?":{}|<>]/.test(p) },
];

const LOCAL_STATES = [
  "WEST", "NORTH", "EAST", "NORTH_EAST", "CENTRAL",
  "NORTH_WEST", "SOUTH_EAST", "UVA", "SABARAGAMUWA",
];

// ── Form shape ────────────────────────────────────────────────────────────────
interface FormValues {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
  addressLine1: string;
  addressLine2: string;
  street: string;
  postalCode: string;
  state: string;
  country: string;
}

type FormErrors = Partial<Record<keyof FormValues | "terms", string>>;

const INITIAL: FormValues = {
  name: "", email: "", phone: "", password: "", confirmPassword: "",
  addressLine1: "", addressLine2: "", street: "", postalCode: "",
  state: "", country: "",
};

// ── Validation ────────────────────────────────────────────────────────────────
function validate(form: FormValues, agreed: boolean): FormErrors {
  const e: FormErrors = {};
  if (!form.name.trim())   e.name  = "Name is required";
  if (!form.email.trim())  e.email = "Email is required";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "Invalid email format";
  if (!form.password)      e.password = "Password is required";
  else if (!PASSWORD_RULES.every(r => r.test(form.password))) e.password = "Password does not meet requirements";
  if (!form.confirmPassword)                      e.confirmPassword = "Please confirm your password";
  else if (form.password !== form.confirmPassword) e.confirmPassword = "Passwords do not match";
  if (!form.addressLine1.trim()) e.addressLine1 = "Address line 1 is required";
  if (!form.country.trim())      e.country      = "Country is required";
  if (!agreed)                   e.terms        = "You must agree to the terms";
  return e;
}

// ── Component ─────────────────────────────────────────────────────────────────
export default function SignUpForm() {
  const navigate = useNavigate();

  const [form, setForm]           = useState<FormValues>(INITIAL);
  const [errors, setErrors]       = useState<FormErrors>({});
  const [agreed, setAgreed]       = useState(false);
  const [showPassword, setShowPassword]   = useState(false);
  const [showConfirm,  setShowConfirm]    = useState(false);
  const [loading, setLoading]     = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  // Generic field change handler
  const fieldChange =
    (key: keyof FormValues) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm(prev => ({ ...prev, [key]: e.target.value }));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate(form, agreed);
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }

    setErrors({});
    setServerError(null);
    setLoading(true);
    try {
      await register({
        name:     form.name.trim(),
        email:    form.email.trim(),
        phone:    form.phone.trim() || undefined,
        password: form.password,
        addresses: [{
          addressLine1: form.addressLine1.trim(),
          addressLine2: form.addressLine2.trim() || undefined,
          street:       form.street.trim()       || undefined,
          postalCode:   form.postalCode.trim()   || undefined,
          state:        form.state               || undefined,
          country:      form.country.trim(),
        }],
      });
      navigate("/signin?registered=true");
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })
          ?.response?.data?.message ?? "Registration failed. Please try again.";
      setServerError(message);
    } finally {
      setLoading(false);
    }
  }

  const inputClass = "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 placeholder:text-gray-400 focus:outline-none focus:border-brand-300 focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800";

  return (
    <div className="flex flex-col flex-1 w-full overflow-y-auto lg:w-1/2 no-scrollbar">
      <div className="w-full max-w-md mx-auto mb-5 sm:pt-10">
        <Link
          to="/"
          className="inline-flex items-center text-sm text-gray-500 transition-colors hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300"
        >
          ← Back
        </Link>
      </div>

      <div className="flex flex-col justify-center flex-1 w-full max-w-md mx-auto">
        <div className="mb-5 sm:mb-8">
          <h1 className="mb-2 font-semibold text-gray-800 text-title-sm dark:text-white/90 sm:text-title-md">
            Create Account
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Fill in the details below to register.
          </p>
        </div>

        {serverError && (
          <div className="mb-4 rounded-lg bg-error-50 border border-error-200 px-4 py-3 text-sm text-error-700 dark:bg-error-500/10 dark:border-error-500/20 dark:text-error-400">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-5">

            {/* ── Personal details ── */}
            <div>
              <Label>Full Name <span className="text-error-500">*</span></Label>
              <Input placeholder="John Doe" value={form.name} onChange={fieldChange("name")} />
              {errors.name && <p className="mt-1 text-xs text-error-500">{errors.name}</p>}
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <div>
                <Label>Email <span className="text-error-500">*</span></Label>
                <Input type="email" placeholder="john@example.com" value={form.email} onChange={fieldChange("email")} />
                {errors.email && <p className="mt-1 text-xs text-error-500">{errors.email}</p>}
              </div>
              <div>
                <Label>Phone</Label>
                <Input type="tel" placeholder="+94771234567" value={form.phone} onChange={fieldChange("phone")} />
              </div>
            </div>

            {/* ── Password ── */}
            <div>
              <Label>Password <span className="text-error-500">*</span></Label>
              <div className="relative">
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="Min 12 chars"
                  value={form.password}
                  onChange={fieldChange("password")}
                />
                <span
                  onClick={() => setShowPassword(v => !v)}
                  className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                >
                  {showPassword
                    ? <EyeIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                    : <EyeCloseIcon className="fill-gray-500 dark:fill-gray-400 size-5" />}
                </span>
              </div>
              {/* Password strength checklist */}
              {form.password && (
                <ul className="mt-2 space-y-1">
                  {PASSWORD_RULES.map(rule => (
                    <li key={rule.label} className={`flex items-center gap-1.5 text-xs ${rule.test(form.password) ? "text-success-500" : "text-gray-400"}`}>
                      <span>{rule.test(form.password) ? "✓" : "○"}</span>
                      {rule.label}
                    </li>
                  ))}
                </ul>
              )}
              {errors.password && <p className="mt-1 text-xs text-error-500">{errors.password}</p>}
            </div>

            <div>
              <Label>Confirm Password <span className="text-error-500">*</span></Label>
              <div className="relative">
                <Input
                  type={showConfirm ? "text" : "password"}
                  placeholder="Repeat password"
                  value={form.confirmPassword}
                  onChange={fieldChange("confirmPassword")}
                />
                <span
                  onClick={() => setShowConfirm(v => !v)}
                  className="absolute z-30 -translate-y-1/2 cursor-pointer right-4 top-1/2"
                >
                  {showConfirm
                    ? <EyeIcon className="fill-gray-500 dark:fill-gray-400 size-5" />
                    : <EyeCloseIcon className="fill-gray-500 dark:fill-gray-400 size-5" />}
                </span>
              </div>
              {errors.confirmPassword && <p className="mt-1 text-xs text-error-500">{errors.confirmPassword}</p>}
            </div>

            {/* ── Address ── */}
            <p className="text-xs font-semibold uppercase tracking-wide text-gray-400 dark:text-gray-500 pt-1">
              Address
            </p>

            <div>
              <Label>Address Line 1 <span className="text-error-500">*</span></Label>
              <Input placeholder="123 Main Street" value={form.addressLine1} onChange={fieldChange("addressLine1")} />
              {errors.addressLine1 && <p className="mt-1 text-xs text-error-500">{errors.addressLine1}</p>}
            </div>

            <div>
              <Label>Address Line 2</Label>
              <Input placeholder="Apt 4B (optional)" value={form.addressLine2} onChange={fieldChange("addressLine2")} />
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <div>
                <Label>Street</Label>
                <Input placeholder="Galle Road" value={form.street} onChange={fieldChange("street")} />
              </div>
              <div>
                <Label>Postal Code</Label>
                <Input placeholder="10100" value={form.postalCode} onChange={fieldChange("postalCode")} />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <div>
                <Label>Province / State</Label>
                <select
                  value={form.state}
                  onChange={fieldChange("state")}
                  className={inputClass}
                >
                  <option value="">Select province</option>
                  {LOCAL_STATES.map(s => (
                    <option key={s} value={s}>{s.replace(/_/g, " ")}</option>
                  ))}
                </select>
              </div>
              <div>
                <Label>Country <span className="text-error-500">*</span></Label>
                <Input placeholder="Sri Lanka" value={form.country} onChange={fieldChange("country")} />
                {errors.country && <p className="mt-1 text-xs text-error-500">{errors.country}</p>}
              </div>
            </div>

            {/* ── Terms ── */}
            <div>
              <div className="flex items-start gap-3">
                <Checkbox checked={agreed} onChange={setAgreed} />
                <p className="text-sm font-normal text-gray-500 dark:text-gray-400">
                  By creating an account you agree to the{" "}
                  <span className="text-gray-800 dark:text-white/90">Terms and Conditions</span>{" "}
                  and our{" "}
                  <span className="text-gray-800 dark:text-white/90">Privacy Policy</span>.
                </p>
              </div>
              {errors.terms && <p className="mt-1 text-xs text-error-500">{errors.terms}</p>}
            </div>

            <Button className="w-full" size="sm" disabled={loading}>
              {loading ? "Creating account..." : "Create Account"}
            </Button>
          </div>
        </form>

        <div className="mt-5 mb-8">
          <p className="text-sm font-normal text-center text-gray-700 dark:text-gray-400 sm:text-start">
            Already have an account?{" "}
            <Link to="/signin" className="text-brand-500 hover:text-brand-600 dark:text-brand-400">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
