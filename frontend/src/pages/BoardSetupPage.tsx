import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useLocation } from "react-router-dom";
import { createCommittee, listCommittees } from "../api/committees";
import { createBoard, listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { inviteDirector, listDirectors } from "../api/directors";
import type { BoardSummary, CommitteeSummary, DirectorSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { CLASSIFICATION_LABELS, canManageBoard } from "../constants/directors";
import { useAuth } from "../context/AuthContext";

export function BoardSetupPage() {
  const { user } = useAuth();
  const location = useLocation();
  const canManage = canManageBoard(user?.role);
  const canInvite = user?.role === "COMPANY_SECRETARY" || user?.role === "EVALUATOR";

  const [board, setBoard] = useState<BoardSummary | null>(null);
  const [directors, setDirectors] = useState<DirectorSummary[]>([]);
  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const notice = (location.state as { notice?: string } | null)?.notice;

  const [boardName, setBoardName] = useState("");
  const [committeeName, setCommitteeName] = useState("");
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);
  const [invitingId, setInvitingId] = useState<string | null>(null);
  const [revealedInvite, setRevealedInvite] = useState<{ name: string; email: string; password: string } | null>(
    null,
  );

  useEffect(() => {
    listBoards()
      .then(async (boards) => {
        if (boards.length === 0) return;
        setBoard(boards[0]);
        const [directorList, committeeList] = await Promise.all([
          listDirectors(boards[0].id),
          listCommittees(boards[0].id),
        ]);
        setDirectors(directorList);
        setCommittees(committeeList);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function guardedSave(action: () => Promise<void>) {
    if (savingRef.current) return;
    savingRef.current = true;
    setSaving(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  function handleCreateBoard(event: FormEvent) {
    event.preventDefault();
    guardedSave(async () => {
      const created = await createBoard({ name: boardName.trim() });
      setBoard(created);
      setBoardName("");
    });
  }

  function handleAddCommittee(event: FormEvent) {
    event.preventDefault();
    if (!board) return;
    guardedSave(async () => {
      const created = await createCommittee({ boardId: board.id, name: committeeName.trim() });
      setCommittees((prev) => [...prev, created]);
      setCommitteeName("");
    });
  }

  async function handleInvite(director: DirectorSummary) {
    if (invitingId) return;
    setError(null);
    setInvitingId(director.id);
    try {
      const result = await inviteDirector(director.id);
      setDirectors((prev) => prev.map((d) => (d.id === director.id ? { ...d, hasPortalAccess: true } : d)));
      setRevealedInvite({ name: director.name, email: result.email, password: result.temporaryPassword });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setInvitingId(null);
    }
  }

  const directorNames = new Map(directors.map((d) => [d.id, d.name]));
  const committeesByDirector = new Map<string, string[]>();
  for (const committee of committees) {
    for (const member of committee.members) {
      const label = member.role === "CHAIR" ? `${committee.name} (Chair)` : committee.name;
      committeesByDirector.set(member.directorId, [...(committeesByDirector.get(member.directorId) ?? []), label]);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Board Setup</h1>
          <p>Directors, their biodata and committee roles.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {notice && <p className="session-notice">{notice}</p>}
        {loading && <p className="page-status">Loading...</p>}

        {revealedInvite && (
          <section className="dashboard-section key-reveal">
            <h2>Portal login for {revealedInvite.name}</h2>
            <p className="form-error">Copy this now &mdash; it won&apos;t be shown again. Share it with them directly.</p>
            <p>
              Email: <code>{revealedInvite.email}</code>
              <br />
              Temporary password: <code>{revealedInvite.password}</code>
            </p>
            <button type="button" className="secondary small" onClick={() => setRevealedInvite(null)}>
              Done
            </button>
          </section>
        )}

        {!loading && !board && (
          <section className="dashboard-section">
            <h2>Create your board</h2>
            {canManage ? (
              <form className="add-form" onSubmit={handleCreateBoard}>
                <label>
                  Board name
                  <input value={boardName} onChange={(e) => setBoardName(e.target.value)} required />
                </label>
                <button type="submit" disabled={saving}>
                  {saving ? "Creating..." : "Create board"}
                </button>
              </form>
            ) : (
              <p className="table-hint">Your Company Secretary hasn&apos;t set up the board yet.</p>
            )}
          </section>
        )}

        {board && (
          <>
            <section className="dashboard-section">
              <div className="section-heading-row">
                <div>
                  <h2>Directors</h2>
                  <p className="table-hint section-subtitle">{board.name}</p>
                </div>
                {canManage && (
                  <Link className="button-link" to="/board-setup/directors/new">
                    Add director
                  </Link>
                )}
              </div>

              {directors.length === 0 ? (
                <p className="table-hint">No directors yet.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Classification</th>
                      <th>Email</th>
                      <th>Committees</th>
                      <th>Portal</th>
                      {canManage && <th aria-label="Actions" />}
                    </tr>
                  </thead>
                  <tbody>
                    {directors.map((d) => (
                      <tr key={d.id}>
                        <td>
                          <Link className="row-link" to={`/board-setup/directors/${d.id}`}>
                            {d.name}
                          </Link>
                        </td>
                        <td>{CLASSIFICATION_LABELS[d.classification]}</td>
                        <td>{d.email ?? <span className="table-hint">Missing &mdash; please add</span>}</td>
                        <td>{committeesByDirector.get(d.id)?.join(", ") ?? <span className="table-hint">&mdash;</span>}</td>
                        <td>
                          {d.hasPortalAccess ? (
                            <span className="table-hint">Access granted</span>
                          ) : canInvite ? (
                            <button
                              type="button"
                              className="secondary small"
                              disabled={!d.email || invitingId !== null}
                              onClick={() => handleInvite(d)}
                              title={d.email ? undefined : "Add an email first"}
                            >
                              {invitingId === d.id ? "Inviting..." : "Invite"}
                            </button>
                          ) : (
                            <span className="table-hint">Not invited</span>
                          )}
                        </td>
                        {canManage && (
                          <td className="cell-actions">
                            <Link className="text-link" to={`/board-setup/directors/${d.id}/edit`}>
                              Edit
                            </Link>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {user?.role === "ORG_ADMIN" && directors.some((d) => !d.hasPortalAccess) && (
                <p className="table-hint form-hint">
                  Portal invitations are sent by a Company Secretary or Evaluator. You can add one on the{" "}
                  <Link className="text-link" to="/users">
                    Users
                  </Link>{" "}
                  page.
                </p>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Committees</h2>
              {committees.length === 0 ? (
                <p className="table-hint">No committees yet.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Committee</th>
                      <th>Chair</th>
                      <th>Members</th>
                    </tr>
                  </thead>
                  <tbody>
                    {committees.map((c) => {
                      const chair = c.members.find((m) => m.role === "CHAIR");
                      const members = c.members.filter((m) => m.role !== "CHAIR");
                      return (
                        <tr key={c.id}>
                          <td>{c.name}</td>
                          <td>
                            {chair ? directorNames.get(chair.directorId) : <span className="table-hint">Not set</span>}
                          </td>
                          <td>
                            {members.length > 0 ? (
                              members.map((m) => directorNames.get(m.directorId)).join(", ")
                            ) : (
                              <span className="table-hint">None</span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
              {canManage && (
                <>
                  <form className="add-form" onSubmit={handleAddCommittee}>
                    <label>
                      Committee name
                      <input value={committeeName} onChange={(e) => setCommitteeName(e.target.value)} required />
                    </label>
                    <button type="submit" disabled={saving}>
                      {saving ? "Adding..." : "Add committee"}
                    </button>
                  </form>
                  <p className="table-hint form-hint">
                    To put a director on a committee or set their role, open the director&apos;s profile.
                  </p>
                </>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
