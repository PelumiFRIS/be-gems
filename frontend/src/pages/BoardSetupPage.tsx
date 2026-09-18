import { useEffect, useState, type FormEvent } from "react";
import { addCommitteeMember, createCommittee, listCommittees } from "../api/committees";
import { createBoard, listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { createDirector, inviteDirector, listDirectors } from "../api/directors";
import type { BoardSummary, CommitteeSummary, DirectorClassification, DirectorSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

const CLASSIFICATIONS: DirectorClassification[] = [
  "CHAIRMAN",
  "CEO_MD",
  "EXECUTIVE_DIRECTOR",
  "NON_EXECUTIVE_DIRECTOR",
  "INDEPENDENT_NON_EXECUTIVE_DIRECTOR",
];

export function BoardSetupPage() {
  const [board, setBoard] = useState<BoardSummary | null>(null);
  const [directors, setDirectors] = useState<DirectorSummary[]>([]);
  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [boardName, setBoardName] = useState("");
  const [directorName, setDirectorName] = useState("");
  const [directorEmail, setDirectorEmail] = useState("");
  const [directorClassification, setDirectorClassification] = useState<DirectorClassification>(
    "NON_EXECUTIVE_DIRECTOR",
  );
  const [committeeName, setCommitteeName] = useState("");
  const [invitingId, setInvitingId] = useState<string | null>(null);
  const [revealedInvite, setRevealedInvite] = useState<{ name: string; email: string; password: string } | null>(
    null,
  );

  useEffect(() => {
    listBoards()
      .then(async (boards) => {
        if (boards.length === 0) {
          setLoading(false);
          return;
        }
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

  async function handleCreateBoard(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const created = await createBoard({ name: boardName });
      setBoard(created);
      setBoardName("");
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddDirector(event: FormEvent) {
    event.preventDefault();
    if (!board) return;
    setError(null);
    try {
      const created = await createDirector({
        boardId: board.id,
        name: directorName,
        email: directorEmail || undefined,
        classification: directorClassification,
      });
      setDirectors((prev) => [...prev, created]);
      setDirectorName("");
      setDirectorEmail("");
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddCommittee(event: FormEvent) {
    event.preventDefault();
    if (!board) return;
    setError(null);
    try {
      const created = await createCommittee({ boardId: board.id, name: committeeName });
      setCommittees((prev) => [...prev, created]);
      setCommitteeName("");
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleInvite(director: DirectorSummary) {
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

  async function handleMakeChair(committeeId: string, directorId: string) {
    setError(null);
    try {
      const updated = await addCommitteeMember(committeeId, directorId, "CHAIR");
      setCommittees((prev) => prev.map((c) => (c.id === committeeId ? updated : c)));
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Board Setup</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

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
            <form className="add-form" onSubmit={handleCreateBoard}>
              <label>
                Board name
                <input value={boardName} onChange={(e) => setBoardName(e.target.value)} required />
              </label>
              <button type="submit">Create board</button>
            </form>
          </section>
        )}

        {board && (
          <>
            <section className="dashboard-section">
              <h2>{board.name}</h2>

              <h3>Directors</h3>
              <ul className="simple-list">
                {directors.map((d) => (
                  <li key={d.id}>
                    {d.name} &mdash; {d.classification.replaceAll("_", " ")}
                    {d.hasPortalAccess ? (
                      <span className="table-hint">Portal access granted</span>
                    ) : (
                      <button
                        type="button"
                        className="secondary small"
                        disabled={!d.email || invitingId === d.id}
                        onClick={() => handleInvite(d)}
                        title={d.email ? undefined : "Add an email first"}
                      >
                        {invitingId === d.id ? "Inviting..." : "Invite to portal"}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
              <form className="add-form" onSubmit={handleAddDirector}>
                <label>
                  Name
                  <input value={directorName} onChange={(e) => setDirectorName(e.target.value)} required />
                </label>
                <label>
                  Email (optional)
                  <input type="email" value={directorEmail} onChange={(e) => setDirectorEmail(e.target.value)} />
                </label>
                <label>
                  Classification
                  <select
                    value={directorClassification}
                    onChange={(e) => setDirectorClassification(e.target.value as DirectorClassification)}
                  >
                    {CLASSIFICATIONS.map((c) => (
                      <option key={c} value={c}>
                        {c.replaceAll("_", " ")}
                      </option>
                    ))}
                  </select>
                </label>
                <button type="submit">Add director</button>
              </form>
            </section>

            <section className="dashboard-section">
              <h3>Committees</h3>
              <ul className="simple-list">
                {committees.map((c) => (
                  <li key={c.id}>
                    {c.name} &mdash; {c.members.length} member{c.members.length === 1 ? "" : "s"}
                    {directors.length > 0 && (
                      <select
                        defaultValue=""
                        onChange={(e) => {
                          if (e.target.value) handleMakeChair(c.id, e.target.value);
                        }}
                      >
                        <option value="" disabled>
                          Set chair...
                        </option>
                        {directors.map((d) => (
                          <option key={d.id} value={d.id}>
                            {d.name}
                          </option>
                        ))}
                      </select>
                    )}
                  </li>
                ))}
              </ul>
              <form className="add-form" onSubmit={handleAddCommittee}>
                <label>
                  Committee name
                  <input value={committeeName} onChange={(e) => setCommitteeName(e.target.value)} required />
                </label>
                <button type="submit">Add committee</button>
              </form>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
