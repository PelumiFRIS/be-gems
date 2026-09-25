import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { AuthShowcase } from "../components/AuthShowcase";
import { PasswordInput } from "../components/PasswordInput";
import frisLogoNavy from "../assets/fris-logo-navy.png";

export function SignupPage() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [organizationName, setOrganizationName] = useState("");
  const [adminFirstName, setAdminFirstName] = useState("");
  const [adminLastName, setAdminLastName] = useState("");
  const [adminEmail, setAdminEmail] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signup({ organizationName, adminFirstName, adminLastName, adminEmail, adminPassword });
      navigate("/dashboard");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <AuthShowcase />
      <div className="auth-form-panel">
        <form className="auth-card" onSubmit={handleSubmit}>
          <img src={frisLogoNavy} alt="First Registrars" className="auth-logo" />
          <p className="auth-eyebrow">BE-GEMS</p>
          <h1>Set up your organisation</h1>
          <p className="auth-subtitle">Create your organisation and administrator account.</p>

          <label>
            Organisation name
            <input value={organizationName} onChange={(e) => setOrganizationName(e.target.value)} required />
          </label>
          <div className="field-row">
            <label>
              First name
              <input value={adminFirstName} onChange={(e) => setAdminFirstName(e.target.value)} required />
            </label>
            <label>
              Last name
              <input value={adminLastName} onChange={(e) => setAdminLastName(e.target.value)} required />
            </label>
          </div>
          <label>
            Email address
            <input type="email" value={adminEmail} onChange={(e) => setAdminEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <PasswordInput
              value={adminPassword}
              onChange={setAdminPassword}
              minLength={8}
              required
              autoComplete="new-password"
            />
          </label>

          {error && <p className="form-error">{error}</p>}

          <button type="submit" disabled={submitting} style={{ width: "100%" }}>
            {submitting ? "Creating..." : "Create organisation"}
          </button>

          <p className="auth-footer">
            Already have an account? <Link to="/login">Sign in &rarr;</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
