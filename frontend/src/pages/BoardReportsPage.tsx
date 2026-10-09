import { useCallback, useEffect, useState } from "react";
import { extractErrorMessage } from "../api/client";
import { listMyBoardReports } from "../api/reportApproval";
import { openEvaluationReport } from "../api/reports";
import type { MyBoardReport, ReportApprovalStatus } from "../api/types";
import { Badge } from "../components/Badge";
import { ReportApprovalPanel } from "../components/ReportApprovalPanel";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

/** Directors read the approved Board Evaluation Report here; the Chairman also approves it here. */
export function BoardReportsPage() {
  const [reports, setReports] = useState<MyBoardReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [opening, setOpening] = useState<string | null>(null);

  useEffect(() => {
    listMyBoardReports()
      .then(setReports)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const handleStatusChange = useCallback((status: ReportApprovalStatus) => {
    setReports((prev) =>
      prev.map((r) =>
        r.evaluationId === status.evaluationId ? { ...r, stage: status.stage, stageLabel: status.stageLabel } : r,
      ),
    );
  }, []);

  async function handleOpen(evaluationId: string) {
    setError(null);
    setOpening(evaluationId);
    const tab = window.open("", "_blank");
    if (tab) {
      tab.document.title = "Preparing report…";
      tab.document.body.textContent = "Preparing report…";
    }
    try {
      await openEvaluationReport(evaluationId, tab);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setOpening(null);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Board Reports</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && reports.length === 0 && (
          <section className="dashboard-section">
            <p className="table-hint">
              The Board Evaluation Report will appear here once it has been approved.
            </p>
          </section>
        )}

        {reports.map((report) => (
          <div key={report.evaluationId}>
            <section className="dashboard-section">
              <div className="section-heading-row">
                <div>
                  <h2>Board Evaluation Report &mdash; {report.year}</h2>
                  <p className="table-hint section-subtitle">
                    {report.boardName} &middot;{" "}
                    <Badge
                      value={report.stage === "FINAL" ? "COMPLETED" : "IN_PROGRESS"}
                      label={report.stage === "FINAL" ? "Final" : `Draft · ${report.stageLabel}`}
                    />
                  </p>
                </div>
                <button
                  type="button"
                  className="secondary small"
                  onClick={() => handleOpen(report.evaluationId)}
                  disabled={opening === report.evaluationId}
                >
                  {opening === report.evaluationId
                    ? "Preparing report..."
                    : report.stage === "FINAL"
                      ? "Open final report"
                      : "Open draft report"}
                </button>
              </div>
            </section>
            {report.awaitingMyApproval && (
              <ReportApprovalPanel evaluationId={report.evaluationId} onStatusChange={handleStatusChange} />
            )}
          </div>
        ))}
      </main>
    </div>
  );
}
