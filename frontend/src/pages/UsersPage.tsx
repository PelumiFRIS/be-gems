import { useEffect, useRef, useState, type FormEvent } from "react";
import { extractErrorMessage } from "../api/client";
import type { Role, UserSummary } from "../api/types";
import {
  changeCompanySecretaryAccess,
  changeUserRole,
  changeUserStatus,
  createUser,
  listUsers,
  resetUserPassword,
} from "../api/users";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";

const ASSIGNABLE_ROLES: Role[] = ["COMPANY_SECRETARY", "EVALUATOR", "ORG_ADMIN"];

interface RevealedPassword {
  heading: string;
  email: string;
  password: string;
}

export function UsersPage() {
  const { user: currentUser, refreshUser } = useAuth();
  const isAdmin = currentUser?.role === "ORG_ADMIN";

  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyUserId, setBusyUserId] = useState<string | null>(null);
  const [revealed, setRevealed] = useState<RevealedPassword | null>(null);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<Role>("COMPANY_SECRETARY");
  const [creating, setCreating] = useState(false);
  const creatingRef = useRef(false);

  useEffect(() => {
    if (!isAdmin) {
      setLoading(false);
      return;
    }
    listUsers()
      .then(setUsers)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [isAdmin]);

  function replaceUser(updated: UserSummary) {
    setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (creatingRef.current) return;
    creatingRef.current = true;
    setCreating(true);
    setError(null);
    try {
      const result = await createUser({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        role,
      });
      setUsers((prev) => [...prev, result.user]);
      setRevealed({
        heading: `Login for ${result.user.firstName} ${result.user.lastName}`,
        email: result.user.email,
        password: result.temporaryPassword,
      });
      setFirstName("");
      setLastName("");
      setEmail("");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      creatingRef.current = false;
      setCreating(false);
    }
  }

  async function runForUser(target: UserSummary, action: () => Promise<void>) {
    if (busyUserId) return;
    setBusyUserId(target.id);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusyUserId(null);
    }
  }

  function handleRoleChange(target: UserSummary, newRole: Role) {
    runForUser(target, async () => replaceUser(await changeUserRole(target.id, newRole)));
  }

  function handleCompanySecretaryAccess(target: UserSummary, enabled: boolean) {
    runForUser(target, async () => {
      replaceUser(await changeCompanySecretaryAccess(target.id, enabled));
      if (target.id === currentUser?.id) await refreshUser();
    });
  }

  function handleToggleStatus(target: UserSummary) {
    const next = target.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    runForUser(target, async () => replaceUser(await changeUserStatus(target.id, next)));
  }

  function handleResetPassword(target: UserSummary) {
    runForUser(target, async () => {
      const result = await resetUserPassword(target.id);
      setRevealed({
        heading: `New password for ${target.firstName} ${target.lastName}`,
        email: result.email,
        password: result.temporaryPassword,
      });
    });
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Users</h1>
          <p>
            Staff accounts for your organisation. Directors get their logins from Board Setup. Tick &ldquo;Also Company
            Secretary&rdquo; for an administrator who also creates and runs evaluations.
          </p>
        </div>

        {!isAdmin && <p className="form-error">Only an Organisation Administrator can manage users.</p>}
        {error && <p className="form-error">{error}</p>}
        {loading && <p className="page-status">Loading...</p>}

        {revealed && (
          <section className="dashboard-section key-reveal">
            <h2>{revealed.heading}</h2>
            <p className="form-error">
              Copy this now &mdash; it won&apos;t be shown again. Share it with them directly and ask them to change it
              from My Account after signing in.
            </p>
            <p>
              Email: <code>{revealed.email}</code>
              <br />
              Temporary password: <code>{revealed.password}</code>
            </p>
            <button type="button" className="secondary small" onClick={() => setRevealed(null)}>
              Done
            </button>
          </section>
        )}

        {isAdmin && !loading && (
          <>
            <section className="dashboard-section">
              <h2>All users</h2>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th aria-label="Actions" />
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const isSelf = u.id === currentUser?.id;
                    const busy = busyUserId === u.id;
                    return (
                      <tr key={u.id}>
                        <td>
                          <strong>
                            {u.firstName} {u.lastName}
                          </strong>
                          {isSelf && <span className="table-hint"> (you)</span>}
                        </td>
                        <td>{u.email}</td>
                        <td>
                          {isSelf || u.role === "DIRECTOR" || u.role === "SUPER_ADMIN" ? (
                            <div>{ROLE_LABELS[u.role]}</div>
                          ) : (
                            <select
                              aria-label={`Role for ${u.firstName} ${u.lastName}`}
                              value={u.role}
                              disabled={busyUserId !== null}
                              onChange={(e) => handleRoleChange(u, e.target.value as Role)}
                            >
                              {ASSIGNABLE_ROLES.map((r) => (
                                <option key={r} value={r}>
                                  {ROLE_LABELS[r]}
                                </option>
                              ))}
                            </select>
                          )}
                          {u.role === "ORG_ADMIN" && (
                            <label className="checkbox-label">
                              <input
                                type="checkbox"
                                checked={u.companySecretaryAccess}
                                disabled={busyUserId !== null}
                                onChange={(e) => handleCompanySecretaryAccess(u, e.target.checked)}
                              />
                              Also Company Secretary
                            </label>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${u.status === "ACTIVE" ? "badge-completed" : "badge-critical"}`}>
                            {u.status === "ACTIVE" ? "Active" : "Disabled"}
                          </span>
                        </td>
                        <td className="cell-actions">
                          {!isSelf && (
                            <div className="row-actions">
                              <button
                                type="button"
                                className="secondary small"
                                disabled={busyUserId !== null}
                                onClick={() => handleResetPassword(u)}
                              >
                                Reset password
                              </button>
                              <button
                                type="button"
                                className="secondary small"
                                disabled={busyUserId !== null}
                                onClick={() => handleToggleStatus(u)}
                              >
                                {busy ? "Saving..." : u.status === "ACTIVE" ? "Disable" : "Enable"}
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </section>

            <section className="dashboard-section">
              <h2>Add a user</h2>
              <p className="table-hint">
                A temporary password is generated for them and shown to you once.
              </p>
              <form className="add-form" onSubmit={handleCreate}>
                <label>
                  First name
                  <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required maxLength={100} />
                </label>
                <label>
                  Last name
                  <input value={lastName} onChange={(e) => setLastName(e.target.value)} required maxLength={100} />
                </label>
                <label>
                  Email
                  <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </label>
                <label>
                  Role
                  <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
                    {ASSIGNABLE_ROLES.map((r) => (
                      <option key={r} value={r}>
                        {ROLE_LABELS[r]}
                      </option>
                    ))}
                  </select>
                </label>
                <button type="submit" disabled={creating}>
                  {creating ? "Adding..." : "Add user"}
                </button>
              </form>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
