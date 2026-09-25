import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { extractErrorMessage, SESSION_EXPIRED_KEY } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { PasswordInput } from "../components/PasswordInput";
import frisLogoNavy from "../assets/fris-logo-navy.png";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sessionExpired] = useState(() => {
    const expired = sessionStorage.getItem(SESSION_EXPIRED_KEY) === "1";
    if (expired) sessionStorage.removeItem(SESSION_EXPIRED_KEY);
    return expired;
  });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ email, password });
      navigate("/dashboard");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <img src={frisLogoNavy} alt="First Registrars" className="auth-logo" />
        <p className="auth-eyebrow">BE-GEMS</p>
        <h1>Welcome back</h1>
        <p className="auth-subtitle">Board Evaluation &amp; Governance Effectiveness Management System</p>

        {sessionExpired && <p className="session-notice">Your session has expired. Please sign in again.</p>}

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <PasswordInput value={password} onChange={setPassword} required autoComplete="current-password" />
        </label>

        {error && <p className="form-error">{error}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? "Signing in..." : "Sign in"}
        </button>

        <p className="auth-footer">
          Setting up a new organisation? <Link to="/signup">Create one</Link>
        </p>
      </form>
    </div>
  );
}
