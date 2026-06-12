import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { extractErrorMessage } from "../api/client";
import AuthShell from "../components/AuthShell";

export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = location.state?.from?.pathname || "/feed";

  if (isAuthenticated) {
    return <Navigate to="/feed" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(identifier.trim(), password);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(extractErrorMessage(err, "Invalid credentials"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell>
      <div className="glass p-8">
        <h1 className="mb-1 font-display text-2xl font-bold tracking-tight text-white">
          Welcome back
        </h1>
        <p className="mb-6 text-sm text-slate-400">Sign in to your DSM account.</p>

        {error && <div className="error-banner mb-4">{error}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="login-identifier" className="field-label">
              Email or username
            </label>
            <input
              id="login-identifier"
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              required
              autoComplete="username"
              className="field"
            />
          </div>
          <div>
            <label htmlFor="login-password" className="field-label">
              Password
            </label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              className="field"
            />
          </div>
          <button type="submit" disabled={submitting} className="btn-primary w-full py-2.5">
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-500">
          New here?{" "}
          <Link to="/signup" className="font-medium text-violet-300 hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}
