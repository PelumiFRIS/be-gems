import { useEffect, useRef, useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { getFinding } from "../api/findings";
import { createAction, listActions, updateAction } from "../api/actions";
import { createRecommendation, listRecommendations, updateRecommendation } from "../api/recommendations";
import { deleteAttachment, downloadAttachment, listAttachments, uploadAttachment } from "../api/attachments";
import type {
  ActionStatus,
  ActionSummary,
  AttachmentSummary,
  FindingSummary,
  RecommendationPriority,
  RecommendationStatus,
  RecommendationSummary,
} from "../api/types";
import { Badge } from "../components/Badge";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { DownloadIcon, PaperclipIcon, TrashIcon } from "../components/icons";
import { formatFileSize } from "../utils/fileSize";

export function FindingDetailPage() {
  const { findingId } = useParams<{ findingId: string }>();
  const [finding, setFinding] = useState<FindingSummary | null>(null);
  const [recommendations, setRecommendations] = useState<RecommendationSummary[]>([]);
  const [actions, setActions] = useState<ActionSummary[]>([]);
  const [attachments, setAttachments] = useState<AttachmentSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [recommendedAction, setRecommendedAction] = useState("");
  const [responsiblePerson, setResponsiblePerson] = useState("");
  const [committeeResponsible, setCommitteeResponsible] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [priority, setPriority] = useState<RecommendationPriority>("MEDIUM");
  const [addingRecommendation, setAddingRecommendation] = useState(false);

  const [actionDescription, setActionDescription] = useState("");
  const [owner, setOwner] = useState("");
  const [approver, setApprover] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [addingAction, setAddingAction] = useState(false);

  useEffect(() => {
    if (!findingId) return;
    Promise.all([
      getFinding(findingId),
      listRecommendations(findingId),
      listActions(findingId),
      listAttachments(findingId),
    ])
      .then(([f, r, a, att]) => {
        setFinding(f);
        setRecommendations(r);
        setActions(a);
        setAttachments(att);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [findingId]);

  async function handleUpload(event: FormEvent) {
    event.preventDefault();
    const file = fileInputRef.current?.files?.[0];
    if (!findingId || !file) return;
    setError(null);
    setUploading(true);
    try {
      const created = await uploadAttachment(findingId, file);
      setAttachments((prev) => [created, ...prev]);
      if (fileInputRef.current) fileInputRef.current.value = "";
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(attachment: AttachmentSummary) {
    setError(null);
    try {
      await deleteAttachment(attachment.id);
      setAttachments((prev) => prev.filter((a) => a.id !== attachment.id));
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleDownload(attachment: AttachmentSummary) {
    setError(null);
    try {
      await downloadAttachment(attachment);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddRecommendation(event: FormEvent) {
    event.preventDefault();
    if (!findingId) return;
    setError(null);
    setAddingRecommendation(true);
    try {
      const created = await createRecommendation(findingId, {
        recommendedAction,
        responsiblePerson: responsiblePerson || undefined,
        committeeResponsible: committeeResponsible || undefined,
        targetDate: targetDate || undefined,
        priority,
        status: "OPEN",
      });
      setRecommendations((prev) => [created, ...prev]);
      setRecommendedAction("");
      setResponsiblePerson("");
      setCommitteeResponsible("");
      setTargetDate("");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setAddingRecommendation(false);
    }
  }

  async function handleRecommendationStatusChange(recommendation: RecommendationSummary, status: RecommendationStatus) {
    setError(null);
    try {
      const updated = await updateRecommendation(recommendation.id, {
        recommendedAction: recommendation.recommendedAction,
        responsiblePerson: recommendation.responsiblePerson ?? undefined,
        committeeResponsible: recommendation.committeeResponsible ?? undefined,
        targetDate: recommendation.targetDate ?? undefined,
        priority: recommendation.priority,
        status,
      });
      setRecommendations((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleAddAction(event: FormEvent) {
    event.preventDefault();
    if (!findingId) return;
    setError(null);
    setAddingAction(true);
    try {
      const created = await createAction(findingId, {
        description: actionDescription,
        owner: owner || undefined,
        approver: approver || undefined,
        dueDate: dueDate || undefined,
      });
      setActions((prev) => [created, ...prev]);
      setActionDescription("");
      setOwner("");
      setApprover("");
      setDueDate("");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setAddingAction(false);
    }
  }

  async function handleActionStatusChange(action: ActionSummary, status: ActionStatus) {
    setError(null);
    try {
      const updated = await updateAction(action.id, {
        description: action.description,
        owner: action.owner ?? undefined,
        approver: action.approver ?? undefined,
        dueDate: action.dueDate ?? undefined,
        status,
        evidence: action.evidence ?? undefined,
      });
      setActions((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
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
          <h1>Finding</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {finding && !loading && (
          <>
            <section className="dashboard-section">
              <div className="finding-card-header">
                <h2 style={{ margin: 0 }}>{finding.description}</h2>
                <Badge value={finding.severity} />
              </div>
              <p className="table-hint">{finding.dimensionName ?? "Board-wide"}</p>
              {finding.evidence && (
                <p>
                  <strong>Evidence:</strong> {finding.evidence}
                </p>
              )}
              {finding.regulatoryReference && (
                <p>
                  <strong>Regulatory reference:</strong> {finding.regulatoryReference}
                </p>
              )}
              {finding.rootCause && (
                <p>
                  <strong>Root cause:</strong> {finding.rootCause}
                </p>
              )}
              {finding.riskImplication && (
                <p>
                  <strong>Risk implication:</strong> {finding.riskImplication}
                </p>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Evidence attachments</h2>
              {attachments.length === 0 && <p className="table-hint">No evidence files uploaded yet.</p>}
              {attachments.length > 0 && (
                <ul className="attachment-list">
                  {attachments.map((attachment) => (
                    <li className="attachment-row" key={attachment.id}>
                      <span className="attachment-icon">
                        <PaperclipIcon width={16} height={16} />
                      </span>
                      <div className="attachment-meta">
                        <div className="attachment-name">{attachment.fileName}</div>
                        <div className="attachment-sub">
                          {formatFileSize(attachment.fileSize)}
                          {attachment.uploadedByName && <> &middot; {attachment.uploadedByName}</>}
                        </div>
                      </div>
                      <div className="attachment-actions">
                        <button type="button" onClick={() => handleDownload(attachment)} title="Download">
                          <DownloadIcon width={16} height={16} />
                        </button>
                        <button
                          type="button"
                          className="danger-ghost"
                          onClick={() => handleDelete(attachment)}
                          title="Delete"
                        >
                          <TrashIcon width={16} height={16} />
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}

              <form className="upload-row" onSubmit={handleUpload}>
                <input ref={fileInputRef} type="file" />
                <button type="submit" disabled={uploading}>
                  {uploading ? "Uploading..." : "Upload evidence"}
                </button>
              </form>
            </section>

            <section className="dashboard-section">
              <h2>Recommendations</h2>
              {recommendations.length === 0 && <p className="table-hint">No recommendations yet.</p>}
              {recommendations.length > 0 && (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Recommended action</th>
                      <th>Responsible</th>
                      <th>Target date</th>
                      <th>Priority</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recommendations.map((recommendation) => (
                      <tr key={recommendation.id}>
                        <td>{recommendation.recommendedAction}</td>
                        <td>{recommendation.responsiblePerson ?? recommendation.committeeResponsible ?? "—"}</td>
                        <td>{recommendation.targetDate ?? "—"}</td>
                        <td>
                          <Badge value={recommendation.priority} />
                        </td>
                        <td>
                          <select
                            value={recommendation.status}
                            onChange={(e) =>
                              handleRecommendationStatusChange(recommendation, e.target.value as RecommendationStatus)
                            }
                          >
                            <option value="OPEN">Open</option>
                            <option value="IN_PROGRESS">In progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="DEFERRED">Deferred</option>
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <form className="add-form" onSubmit={handleAddRecommendation}>
                <label style={{ minWidth: "100%" }}>
                  Recommended action
                  <textarea
                    value={recommendedAction}
                    onChange={(e) => setRecommendedAction(e.target.value)}
                    rows={2}
                    required
                  />
                </label>
                <label>
                  Responsible person
                  <input value={responsiblePerson} onChange={(e) => setResponsiblePerson(e.target.value)} />
                </label>
                <label>
                  Committee responsible
                  <input value={committeeResponsible} onChange={(e) => setCommitteeResponsible(e.target.value)} />
                </label>
                <label>
                  Target date
                  <input type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
                </label>
                <label>
                  Priority
                  <select value={priority} onChange={(e) => setPriority(e.target.value as RecommendationPriority)}>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                  </select>
                </label>
                <button type="submit" disabled={addingRecommendation}>
                  {addingRecommendation ? "Adding..." : "Add recommendation"}
                </button>
              </form>
            </section>

            <section className="dashboard-section">
              <h2>Corrective actions</h2>
              {actions.length === 0 && <p className="table-hint">No actions yet.</p>}
              {actions.length > 0 && (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Action</th>
                      <th>Owner</th>
                      <th>Due date</th>
                      <th>Status</th>
                      <th>Closure date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {actions.map((action) => (
                      <tr key={action.id} className={action.overdue ? "overdue" : undefined}>
                        <td>{action.description}</td>
                        <td>{action.owner ?? "—"}</td>
                        <td>
                          {action.dueDate ?? "—"} {action.overdue && <Badge value="OVERDUE" />}
                        </td>
                        <td>
                          <select
                            value={action.status}
                            onChange={(e) => handleActionStatusChange(action, e.target.value as ActionStatus)}
                          >
                            <option value="NOT_STARTED">Not started</option>
                            <option value="IN_PROGRESS">In progress</option>
                            <option value="COMPLETED">Completed</option>
                          </select>
                        </td>
                        <td>{action.closureDate ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              <form className="add-form" onSubmit={handleAddAction}>
                <label style={{ minWidth: "100%" }}>
                  Action
                  <textarea
                    value={actionDescription}
                    onChange={(e) => setActionDescription(e.target.value)}
                    rows={2}
                    required
                  />
                </label>
                <label>
                  Owner
                  <input value={owner} onChange={(e) => setOwner(e.target.value)} />
                </label>
                <label>
                  Approver
                  <input value={approver} onChange={(e) => setApprover(e.target.value)} />
                </label>
                <label>
                  Due date
                  <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
                </label>
                <button type="submit" disabled={addingAction}>
                  {addingAction ? "Adding..." : "Add action"}
                </button>
              </form>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
