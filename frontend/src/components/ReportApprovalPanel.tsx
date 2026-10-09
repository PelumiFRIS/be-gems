import { useEffect, useState } from "react";
import { extractErrorMessage } from "../api/client";
import { approveReport, getReportApproval, returnReport } from "../api/reportApproval";
import type { ReportApprovalStatus, ReportStage } from "../api/types";

const WAITING_FOR: Record<ReportStage, string> = {
  EVALUATOR_REVIEW: "Waiting for an Evaluator or the Company Secretary to review the results.",
  DRAFT_REPORT: "Waiting for an Evaluator or the Company Secretary to complete the draft report.",
  QUALITY_REVIEW: "Waiting for an Evaluator or the Company Secretary to complete the quality review.",
  CS_REVIEW: "Waiting for the Company Secretary's review.",
  BOARD_APPROVAL: "Waiting for the Chairman's approval, or for the Company Secretary to record the Board's approval.",
  FINAL: "",
};

function returnTarget(stage: ReportStage): string {
  return stage === "DRAFT_REPORT" ? "Evaluator Review" : "Draft Report";
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface ReportApprovalPanelProps {
  evaluationId: string;
  /** Must be stable (e.g. a state setter); the status is reloaded when it changes. */
  onStatusChange?: (status: ReportApprovalStatus) => void;
}

/** Memo §32: the approval stepper, history and the approve/return actions open to the signed-in user. */
export function ReportApprovalPanel({ evaluationId, onStatusChange }: ReportApprovalPanelProps) {
  const [status, setStatus] = useState<ReportApprovalStatus | null>(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getReportApproval(evaluationId)
      .then((loaded) => {
        setStatus(loaded);
        onStatusChange?.(loaded);
      })
      .catch((err) => setError(extractErrorMessage(err)));
  }, [evaluationId, onStatusChange]);

  async function decide(action: typeof approveReport) {
    setError(null);
    setBusy(true);
    try {
      const updated = await action(evaluationId, comment.trim());
      setStatus(updated);
      setComment("");
      onStatusChange?.(updated);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (!status) {
    return error ? <p className="form-error">{error}</p> : null;
  }

  const currentIndex = status.stages.findIndex((s) => s.stage === status.stage);
  const isFinal = status.stage === "FINAL";
  const hasComment = comment.trim().length > 0;

  return (
    <section className="dashboard-section approval-panel">
      <h2>Report approval</h2>
      <p className="table-hint section-subtitle">
        {isFinal
          ? "The report has been approved and issued as final."
          : `The report is a draft at the ${status.stageLabel} stage.`}
      </p>

      <ol className="approval-steps">
        {status.stages.map((step, index) => {
          const state =
            index < currentIndex || isFinal ? "done" : index === currentIndex ? "current" : "upcoming";
          return (
            <li key={step.stage} className={`approval-step approval-step-${state}`}>
              <span className="approval-step-marker">{state === "done" ? "✓" : index + 1}</span>
              <span className="approval-step-label">{step.label}</span>
            </li>
          );
        })}
      </ol>

      {error && <p className="form-error">{error}</p>}

      {status.canApprove || status.canReturn ? (
        <div className="approval-actions">
          <label>
            {status.commentRequired
              ? "Board approval record (required) — for example, the date of the Board meeting"
              : "Comment (required when returning for changes)"}
            <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2} maxLength={2000} />
          </label>
          <div className="button-row">
            {status.canApprove && (
              <button
                type="button"
                onClick={() => decide(approveReport)}
                disabled={busy || (status.commentRequired && !hasComment)}
              >
                {status.approveLabel}
              </button>
            )}
            {status.canReturn && (
              <button type="button" className="secondary" onClick={() => decide(returnReport)} disabled={busy || !hasComment}>
                Return to {returnTarget(status.stage)}
              </button>
            )}
          </div>
        </div>
      ) : (
        !isFinal && <p className="table-hint">{WAITING_FOR[status.stage]}</p>
      )}

      {status.history.length > 0 && (
        <>
          <h3 className="approval-history-heading">Approval history</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Stage</th>
                <th>Decision</th>
                <th>By</th>
                <th>Date</th>
                <th>Comment</th>
              </tr>
            </thead>
            <tbody>
              {status.history.map((event) => (
                <tr key={`${event.createdAt}-${event.fromStage}`}>
                  <td>{event.fromStageLabel}</td>
                  <td>
                    {event.decision === "APPROVED" ? (
                      <span className="badge badge-completed">Approved</span>
                    ) : (
                      <span className="badge badge-deferred">Returned to {event.toStageLabel}</span>
                    )}
                  </td>
                  <td>{event.actorName}</td>
                  <td>{formatDateTime(event.createdAt)}</td>
                  <td className="table-hint">{event.comment ?? ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}
