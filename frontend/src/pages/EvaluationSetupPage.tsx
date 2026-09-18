import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { listDirectors } from "../api/directors";
import { addRespondent, closeEvaluation, getEvaluation, launchEvaluation } from "../api/evaluations";
import type { ConfidentialityMode, DirectorSummary, EvaluationDetail } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

const CONFIDENTIALITY_MODES: ConfidentialityMode[] = ["IDENTIFIED", "CONFIDENTIAL", "ANONYMOUS"];

export function EvaluationSetupPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<EvaluationDetail | null>(null);
  const [directors, setDirectors] = useState<DirectorSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [respondentDirectorId, setRespondentDirectorId] = useState("");
  const [confidentialityMode, setConfidentialityMode] = useState<ConfidentialityMode>("CONFIDENTIAL");

  useEffect(() => {
    if (!id) return;
    getEvaluation(id)
      .then(async (d) => {
        setDetail(d);
        const directorList = await listDirectors(d.evaluation.boardId);
        setDirectors(directorList);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id]);

  const respondentDirectorIds = new Set(detail?.respondents.map((r) => r.directorId));
  const eligibleDirectors = directors.filter((d) => !respondentDirectorIds.has(d.id));

  async function handleAddRespondent(event: FormEvent) {
    event.preventDefault();
    if (!id || !respondentDirectorId) return;
    setError(null);
    try {
      const updated = await addRespondent(id, { directorId: respondentDirectorId, confidentialityMode });
      setDetail(updated);
      setRespondentDirectorId("");
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleLaunch() {
    if (!id) return;
    setError(null);
    setBusy(true);
    try {
      const evaluation = await launchEvaluation(id);
      setDetail((prev) => (prev ? { ...prev, evaluation } : prev));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleClose() {
    if (!id) return;
    setError(null);
    setBusy(true);
    try {
      const evaluation = await closeEvaluation(id);
      setDetail((prev) => (prev ? { ...prev, evaluation } : prev));
      navigate(`/evaluations/${id}/results`);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Evaluation Setup</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {detail && (
          <>
            <section className="dashboard-section">
              <h2>
                {detail.evaluation.evaluationType === "BOARD"
                  ? "Board Evaluation"
                  : `Peer Evaluation of ${detail.evaluation.subjectDirectorName ?? "?"}`}{" "}
                &mdash; {detail.evaluation.year}
              </h2>
              <p className="table-hint">Status: {detail.evaluation.status}</p>

              <h3>Respondents</h3>
              <ul className="simple-list">
                {detail.respondents.map((r) => (
                  <li key={r.id}>
                    {r.directorName} &mdash; {r.confidentialityMode}
                    <span className="table-hint">{r.status}</span>
                  </li>
                ))}
                {detail.respondents.length === 0 && <li>No respondents yet.</li>}
              </ul>

              {detail.evaluation.status === "DRAFT" && (
                <form className="add-form" onSubmit={handleAddRespondent}>
                  <label>
                    Director
                    <select value={respondentDirectorId} onChange={(e) => setRespondentDirectorId(e.target.value)}>
                      <option value="" disabled>
                        Choose a director...
                      </option>
                      {eligibleDirectors.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Confidentiality
                    <select
                      value={confidentialityMode}
                      onChange={(e) => setConfidentialityMode(e.target.value as ConfidentialityMode)}
                    >
                      {CONFIDENTIALITY_MODES.map((mode) => (
                        <option key={mode} value={mode}>
                          {mode}
                        </option>
                      ))}
                    </select>
                  </label>
                  <button type="submit" disabled={!respondentDirectorId}>
                    Add respondent
                  </button>
                </form>
              )}

              {detail.evaluation.status === "DRAFT" && (
                <button type="button" onClick={handleLaunch} disabled={busy || detail.respondents.length === 0}>
                  {busy ? "Launching..." : "Launch evaluation"}
                </button>
              )}
              {detail.evaluation.status === "LAUNCHED" && (
                <button type="button" onClick={handleClose} disabled={busy}>
                  {busy ? "Closing..." : "Close evaluation"}
                </button>
              )}
              {(detail.evaluation.status === "CLOSED" || detail.evaluation.status === "SCORED") && (
                <Link to={`/evaluations/${id}/results`}>
                  <button type="button">View results</button>
                </Link>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
