import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { addCommitteeMember, createCommittee, listCommittees, removeCommitteeMember } from "../api/committees";
import {
  deleteDirector,
  deleteDirectorCv,
  downloadDirectorCv,
  getDirector,
  uploadDirectorCv,
} from "../api/directors";
import type { CommitteeMemberRole, CommitteeSummary, DirectorDetail } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { DownloadIcon, PaperclipIcon, TrashIcon } from "../components/icons";
import { CLASSIFICATION_LABELS, COMMITTEE_ROLE_LABELS, canManageBoard } from "../constants/directors";
import { useAuth } from "../context/AuthContext";
import { formatFileSize } from "../utils/fileSize";

const NEW_COMMITTEE = "__new__";

function formatDate(value: string | null): string | null {
  if (!value) return null;
  return new Date(`${value}T00:00:00`).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function Field({ label, value, wide }: { label: string; value: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? "detail-field span-2" : "detail-field"}>
      <dt>{label}</dt>
      <dd>{value || <span className="table-hint">Not provided</span>}</dd>
    </div>
  );
}

export function DirectorProfilePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const canManage = canManageBoard(user?.role);

  const [director, setDirector] = useState<DirectorDetail | null>(null);
  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(
    (location.state as { notice?: string } | null)?.notice ?? null,
  );
  const [busy, setBusy] = useState(false);
  const [confirmingRemove, setConfirmingRemove] = useState(false);

  const cvInputRef = useRef<HTMLInputElement>(null);
  const [selectedCommittee, setSelectedCommittee] = useState("");
  const [newCommitteeName, setNewCommitteeName] = useState("");
  const [newRole, setNewRole] = useState<CommitteeMemberRole>("MEMBER");

  const refresh = useCallback(async () => {
    if (!id) return;
    const detail = await getDirector(id);
    setDirector(detail);
    if (canManage) {
      setCommittees(await listCommittees(detail.boardId));
    }
  }, [id, canManage]);

  useEffect(() => {
    refresh()
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [refresh]);

  async function run(action: () => Promise<void>) {
    if (busy) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await action();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function handleUploadCv(event: FormEvent) {
    event.preventDefault();
    const file = cvInputRef.current?.files?.[0];
    if (!director || !file) return;
    run(async () => {
      const cv = await uploadDirectorCv(director.id, file);
      setDirector({ ...director, cv });
      if (cvInputRef.current) cvInputRef.current.value = "";
    });
  }

  function handleDeleteCv() {
    if (!director) return;
    run(async () => {
      await deleteDirectorCv(director.id);
      setDirector({ ...director, cv: null });
    });
  }

  function handleDownloadCv() {
    if (!director?.cv) return;
    const fileName = director.cv.fileName;
    run(() => downloadDirectorCv(director.id, fileName));
  }

  function handleAddCommittee(event: FormEvent) {
    event.preventDefault();
    if (!director) return;
    run(async () => {
      let committeeId = selectedCommittee;
      if (selectedCommittee === NEW_COMMITTEE) {
        const created = await createCommittee({ boardId: director.boardId, name: newCommitteeName.trim() });
        committeeId = created.id;
      }
      await addCommitteeMember(committeeId, director.id, newRole);
      await refresh();
      setSelectedCommittee("");
      setNewCommitteeName("");
      setNewRole("MEMBER");
    });
  }

  function handleRoleChange(committeeId: string, role: CommitteeMemberRole) {
    if (!director) return;
    run(async () => {
      await addCommitteeMember(committeeId, director.id, role);
      await refresh();
    });
  }

  function handleRemoveFromCommittee(committeeId: string) {
    if (!director) return;
    run(async () => {
      await removeCommitteeMember(committeeId, director.id);
      await refresh();
    });
  }

  function handleRemoveDirector() {
    if (!director) return;
    run(async () => {
      await deleteDirector(director.id);
      navigate("/board-setup", { state: { notice: `${director.name} was removed from the board.` } });
    });
  }

  const memberOf = new Set(director?.committees.map((c) => c.committeeId));
  const availableCommittees = committees.filter((c) => !memberOf.has(c.id));
  const chairByCommittee = new Map(
    committees.map((c) => [c.id, c.members.find((m) => m.role === "CHAIR")?.directorId ?? null]),
  );

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <nav className="breadcrumb">
          <Link to="/board-setup">Board Setup</Link>
          <span>/</span>
          <span>{director?.name ?? "Director"}</span>
        </nav>

        {error && <p className="form-error">{error}</p>}
        {notice && <p className="session-notice">{notice}</p>}
        {loading && <p className="page-status">Loading...</p>}

        {director && !loading && (
          <>
            <div className="profile-header">
              <div>
                <h1>{director.name}</h1>
                <div className="profile-header-meta">
                  <span className="badge badge-medium">{CLASSIFICATION_LABELS[director.classification]}</span>
                  {director.hasPortalAccess ? (
                    <span className="badge badge-completed">Portal access</span>
                  ) : (
                    <span className="badge badge-open">No portal access</span>
                  )}
                </div>
              </div>
              {canManage && (
                <div className="profile-header-actions">
                  <button type="button" className="secondary" onClick={() => setConfirmingRemove(true)} disabled={busy}>
                    <TrashIcon width={15} height={15} /> Remove
                  </button>
                  <Link className="button-link" to={`/board-setup/directors/${director.id}/edit`}>
                    Edit biodata
                  </Link>
                </div>
              )}
            </div>

            {confirmingRemove && (
              <section className="dashboard-section confirm-panel">
                <h2>Remove {director.name} from the board?</h2>
                <p>
                  Use this for entries added by mistake, such as duplicates. Their committee places, CV and portal
                  login are removed too. Directors who are part of an evaluation can&apos;t be removed &mdash; edit
                  their details instead.
                </p>
                <div className="form-actions">
                  <button type="button" className="secondary" onClick={() => setConfirmingRemove(false)} disabled={busy}>
                    Cancel
                  </button>
                  <button type="button" className="danger" onClick={handleRemoveDirector} disabled={busy}>
                    {busy ? "Removing..." : "Remove director"}
                  </button>
                </div>
              </section>
            )}

            <section className="dashboard-section">
              <h2>Biodata</h2>
              <dl className="detail-grid">
                <Field label="Email" value={director.email} />
                <Field label="Telephone" value={director.phone} />
                <Field label="Date of birth" value={formatDate(director.dateOfBirth)} />
                <Field label="Profession" value={director.profession} />
                <Field label="Address" value={director.address} wide />
              </dl>
            </section>

            <section className="dashboard-section">
              <h2>Board appointment</h2>
              <dl className="detail-grid">
                <Field label="Classification" value={CLASSIFICATION_LABELS[director.classification]} />
                <Field label="Date of first appointment" value={formatDate(director.appointmentDate)} />
                <Field label="Date re-elected" value={formatDate(director.reElectionDate)} />
                <Field label="Term expiration date" value={formatDate(director.termExpirationDate)} />
              </dl>
            </section>

            <section className="dashboard-section">
              <h2>Qualifications and experience</h2>
              <dl className="detail-grid">
                <Field label="Qualification / education" value={director.qualification} wide />
                <Field label="Experience" value={director.experience} wide />
              </dl>
            </section>

            <section className="dashboard-section">
              <h2>Curriculum vitae</h2>
              {director.cv ? (
                <ul className="attachment-list">
                  <li className="attachment-row">
                    <span className="attachment-icon">
                      <PaperclipIcon width={16} height={16} />
                    </span>
                    <div className="attachment-meta">
                      <div className="attachment-name">{director.cv.fileName}</div>
                      <div className="attachment-sub">
                        {formatFileSize(director.cv.fileSize)} &middot; uploaded{" "}
                        {new Date(director.cv.uploadedAt).toLocaleDateString("en-GB")}
                      </div>
                    </div>
                    <div className="attachment-actions">
                      <button type="button" onClick={handleDownloadCv} title="Download CV" disabled={busy}>
                        <DownloadIcon width={16} height={16} />
                      </button>
                      {canManage && (
                        <button
                          type="button"
                          className="danger-ghost"
                          onClick={handleDeleteCv}
                          title="Remove CV"
                          disabled={busy}
                        >
                          <TrashIcon width={16} height={16} />
                        </button>
                      )}
                    </div>
                  </li>
                </ul>
              ) : (
                <p className="table-hint">No CV uploaded yet.</p>
              )}
              {canManage && (
                <form className="upload-row" onSubmit={handleUploadCv}>
                  <input ref={cvInputRef} type="file" accept=".pdf,.doc,.docx" aria-label="CV file" />
                  <button type="submit" disabled={busy}>
                    {director.cv ? "Replace CV" : "Upload CV"}
                  </button>
                </form>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Committees</h2>
              {director.committees.length === 0 && (
                <p className="table-hint">Not on any committee yet.</p>
              )}
              {director.committees.length > 0 && (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Committee</th>
                      <th>Role</th>
                      {canManage && <th aria-label="Actions" />}
                    </tr>
                  </thead>
                  <tbody>
                    {director.committees.map((membership) => (
                      <tr key={membership.committeeId}>
                        <td>{membership.committeeName}</td>
                        <td>
                          {canManage ? (
                            <select
                              aria-label={`Role on ${membership.committeeName}`}
                              value={membership.role}
                              disabled={busy}
                              onChange={(e) =>
                                handleRoleChange(membership.committeeId, e.target.value as CommitteeMemberRole)
                              }
                            >
                              <option value="CHAIR">{COMMITTEE_ROLE_LABELS.CHAIR}</option>
                              <option value="MEMBER">{COMMITTEE_ROLE_LABELS.MEMBER}</option>
                            </select>
                          ) : (
                            COMMITTEE_ROLE_LABELS[membership.role]
                          )}
                        </td>
                        {canManage && (
                          <td className="cell-actions">
                            <button
                              type="button"
                              className="secondary small"
                              disabled={busy}
                              onClick={() => handleRemoveFromCommittee(membership.committeeId)}
                            >
                              Remove
                            </button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {canManage && (
                <form className="add-form" onSubmit={handleAddCommittee}>
                  <label>
                    Committee
                    <select value={selectedCommittee} onChange={(e) => setSelectedCommittee(e.target.value)} required>
                      <option value="" disabled>
                        Select a committee...
                      </option>
                      {availableCommittees.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                      <option value={NEW_COMMITTEE}>+ Create a new committee</option>
                    </select>
                  </label>
                  {selectedCommittee === NEW_COMMITTEE && (
                    <label>
                      New committee name
                      <input
                        value={newCommitteeName}
                        onChange={(e) => setNewCommitteeName(e.target.value)}
                        placeholder="e.g. Audit Committee"
                        required
                      />
                    </label>
                  )}
                  <label>
                    Role
                    <select value={newRole} onChange={(e) => setNewRole(e.target.value as CommitteeMemberRole)}>
                      <option value="MEMBER">{COMMITTEE_ROLE_LABELS.MEMBER}</option>
                      <option value="CHAIR">{COMMITTEE_ROLE_LABELS.CHAIR}</option>
                    </select>
                  </label>
                  <button type="submit" disabled={busy}>
                    {busy ? "Saving..." : "Add to committee"}
                  </button>
                  {newRole === "CHAIR" &&
                    selectedCommittee &&
                    selectedCommittee !== NEW_COMMITTEE &&
                    chairByCommittee.get(selectedCommittee) && (
                      <p className="table-hint form-hint">
                        This committee already has a chair; they&apos;ll become a member.
                      </p>
                    )}
                </form>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
