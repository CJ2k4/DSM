import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { extractErrorMessage } from "../api/client";
import AuthShell from "../components/AuthShell";

const USERNAME_PATTERN = /^[A-Za-z0-9_.]+$/;

export default function SignupPage() {
  const { isAuthenticated, register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    username: "",
    email: "",
    password: "",
    displayName: "",
  });
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/feed" replace />;
  }

  function update(field) {
    return (event) => setForm((prev) => ({ ...prev, [field]: event.target.value }));
  }

  function validate() {
    if (form.username.length < 3 || form.username.length > 30) {
      return "Username must be between 3 and 30 characters.";
    }
    if (!USERNAME_PATTERN.test(form.username)) {
      return "Username can contain letters, numbers, underscores, and dots.";
    }
    if (form.password.length < 8) {
      return "Password must be at least 8 characters.";
    }
    return null;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await register({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        displayName: form.displayName.trim() || form.username.trim(),
      });
      navigate("/feed", { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, "Could not create account"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell>
      <div className="glass p-8">
        <h1 className="mb-1 font-display text-2xl font-bold tracking-tight text-white">
          Join DSM
        </h1>
        <p className="mb-6 text-sm text-slate-400">Create your account.</p>

        {error && <div className="error-banner mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="signup-display-name" className="field-label">
              Display name
            </label>
            <input
              id="signup-display-name"
              type="text"
              value={form.displayName}
              onChange={update("displayName")}
              maxLength={80}
              className="field"
            />
          </div>
          <div>
            <label htmlFor="signup-username" className="field-label">
              Username
            </label>
            <input
              id="signup-username"
              type="text"
              value={form.username}
              onChange={update("username")}
              required
              className="field"
            />
          </div>
          <div>
            <label htmlFor="signup-email" className="field-label">
              Email
            </label>
            <input
              id="signup-email"
              type="email"
              value={form.email}
              onChange={update("email")}
              required
              autoComplete="email"
              className="field"
            />
          </div>
          <div>
            <label htmlFor="signup-password" className="field-label">
              Password
            </label>
            <input
              id="signup-password"
              type="password"
              value={form.password}
              onChange={update("password")}
              required
              autoComplete="new-password"
              className="field"
            />
            <p className="mt-1.5 text-xs text-slate-500">At least 8 characters.</p>
          </div>
          <button type="submit" disabled={submitting} className="btn-primary w-full py-2.5">
            {submitting ? "Creating account…" : "Sign up"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-violet-300 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}
