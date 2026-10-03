import { useState, type FormEvent } from "react";
import { extractErrorMessage } from "../api/client";
import { changeOwnPassword } from "../api/users";
import { PasswordInput } from "../components/PasswordInput";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";

export function AccountPage() {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (saving) return;
    setError(null);
    setSuccess(false);
    if (newPassword !== confirmPassword) {
      setError("The new passwords don't match");
      return;
    }
    setSaving(true);
    try {
      await changeOwnPassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccess(true);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>My Account</h1>
        </div>

        {user && (
          <section className="dashboard-section">
            <h2>Your details</h2>
            <dl className="detail-grid">
              <div className="detail-field">
                <dt>Name</dt>
                <dd>
                  {user.firstName} {user.lastName}
                </dd>
              </div>
              <div className="detail-field">
                <dt>Email</dt>
                <dd>{user.email}</dd>
              </div>
              <div className="detail-field">
                <dt>Role</dt>
                <dd>{ROLE_LABELS[user.role]}</dd>
              </div>
              <div className="detail-field">
                <dt>Organisation</dt>
                <dd>{user.organizationName}</dd>
              </div>
            </dl>
          </section>
        )}

        <section className="dashboard-section narrow-section">
          <h2>Change password</h2>
          <p className="table-hint">
            If you signed in with a temporary password from your administrator, set your own here.
          </p>
          {error && <p className="form-error">{error}</p>}
          {success && <p className="session-notice">Your password has been changed.</p>}
          <form onSubmit={handleSubmit}>
            <label>
              Current password
              <PasswordInput
                value={currentPassword}
                onChange={setCurrentPassword}
                required
                autoComplete="current-password"
              />
            </label>
            <label>
              New password (at least 8 characters)
              <PasswordInput
                value={newPassword}
                onChange={setNewPassword}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </label>
            <label>
              Confirm new password
              <PasswordInput
                value={confirmPassword}
                onChange={setConfirmPassword}
                required
                minLength={8}
                autoComplete="new-password"
              />
            </label>
            <button type="submit" disabled={saving}>
              {saving ? "Saving..." : "Change password"}
            </button>
          </form>
        </section>
      </main>
    </div>
  );
}
