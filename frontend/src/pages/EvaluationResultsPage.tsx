import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { getEvaluation } from "../api/evaluations";
import { createFinding, listFindings } from "../api/findings";
import { calculateScores, getScores } from "../api/scores";
import type { EvaluationDetail, FindingSeverity, FindingSummary, ScoreRowSummary } from "../api/types";
import { Badge } from "../components/Badge";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

export function EvaluationResultsPage() {
  const { id } = useParams<{ id: string }>();
  const [detail, setDetail] = useState<EvaluationDetail | null>(null);
  const [scores, setScores] = useState<ScoreRowSummary[]>([]);
  const [findings, setFindings] = useState<FindingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [calculating, setCalculating] = useState(false);

  const [description, setDescription] = useState("");
  const [severity, setSeverity] = useState<FindingSeverity>("MEDIUM");
  const [evidence, setEvidence] = useState("");
  const [regulatoryReference, setRegulatoryReference] = useState("");
  const [rootCause, setRootCause] = useState("");
  const [riskImplication, setRiskImplication] = useState("");
  const [addingFinding, setAddingFinding] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([getEvaluation(id), getScores(id), listFindings(id)])
      .then(([d, s, f]) => {
        setDetail(d);
        setScores(s);
        setFindings(f);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleCalculate() {
    if (!id) return;
    setError(null);
    setCalculating(true);
    try {
      const computed = await calculateScores(id);
      setScores(computed);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setCalculating(false);
    }
  }

  async function handleAddFinding(event: FormEvent) {
    event.preventDefault();
    if (!id) return;
    setError(null);
    setAddingFinding(true);
    try {
      const created = await createFinding(id, {
        description,
        severity,
        evidence: evidence || undefined,
        regulatoryReference: regulatoryReference || undefined,
        rootCause: rootCause || undefined,
        riskImplication: riskImplication || undefined,
      });
      setFindings((prev) => [created, ...prev]);
      setDescription("");
      setEvidence("");
      setRegulatoryReference("");
      setRootCause("");
      setRiskImplication("");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setAddingFinding(false);
    }
  }

  const dimensionScores = scores.filter((s) => s.scopeType === "DIMENSION");
  const boardOverall = scores.find((s) => s.scopeType === "BOARD_OVERALL");
  const directorOverall = scores.find((s) => s.scopeType === "DIRECTOR_OVERALL");
  const bgeiCategories = scores.filter((s) => s.scopeType === "BGEI_CATEGORY");
  const bgeiOverall = scores.find((s) => s.scopeType === "BGEI_OVERALL");

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Evaluation Results</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {detail && !loading && (
          <>
            <section className="dashboard-section">
              <h2>
                {detail.evaluation.evaluationType === "BOARD"
                  ? "Board Evaluation"
                  : `Peer Evaluation of ${detail.evaluation.subjectDirectorName ?? "?"}`}{" "}
                &mdash; {detail.evaluation.year}
              </h2>
              <p className="table-hint">Status: {detail.evaluation.status}</p>

              {scores.length === 0 && detail.evaluation.status === "CLOSED" && (
                <button type="button" onClick={handleCalculate} disabled={calculating}>
                  {calculating ? "Calculating..." : "Calculate scores"}
                </button>
              )}
              {scores.length === 0 && detail.evaluation.status !== "CLOSED" && (
                <p>Scores are available once the evaluation is closed.</p>
              )}
            </section>

            {dimensionScores.length > 0 && (
              <section className="dashboard-section">
                <h2>Dimension scores</h2>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Dimension</th>
                      <th>Score</th>
                      {detail.evaluation.evaluationType === "BOARD" && <th>Weighted</th>}
                      <th>Maturity</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dimensionScores.map((row) => (
                      <tr key={row.dimensionId}>
                        <td>{row.dimensionName}</td>
                        <td>{row.rawScore}</td>
                        {detail.evaluation.evaluationType === "BOARD" && <td>{row.weightedScore}</td>}
                        <td>{row.maturityLabel ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            )}

            {boardOverall && (
              <section className="dashboard-section">
                <h2>Overall Board Score</h2>
                <p>
                  <strong>{boardOverall.rawScore} / 5.00</strong> &mdash; {boardOverall.maturityLabel}
                </p>
              </section>
            )}

            {directorOverall && (
              <section className="dashboard-section">
                <h2>Overall Director Score</h2>
                <p>
                  <strong>{directorOverall.rawScore} / 5.00</strong>
                </p>
              </section>
            )}

            {bgeiCategories.length > 0 && (
              <section className="dashboard-section">
                <h2>Board Governance Effectiveness Index</h2>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      <th>Score</th>
                      <th>Weighted %</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bgeiCategories.map((row) => (
                      <tr key={row.bgeiCategory}>
                        <td>{row.bgeiCategory}</td>
                        <td>{row.rawScore}</td>
                        <td>{row.weightedScore}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {bgeiOverall && (
                  <p>
                    <strong>
                      {bgeiOverall.weightedScore}% &mdash; {bgeiOverall.bgeiBandLabel}
                    </strong>
                  </p>
                )}
              </section>
            )}

            {scores.length > 0 && (
              <section className="dashboard-section">
                <h2>Findings</h2>

                {findings.length === 0 && <p className="table-hint">No findings recorded yet.</p>}
                {findings.map((finding) => (
                  <div className="finding-card" key={finding.id}>
                    <div className="finding-card-header">
                      <Link to={`/findings/${finding.id}`}>{finding.description}</Link>
                      <Badge value={finding.severity} />
                    </div>
                    <p className="table-hint">
                      {finding.dimensionName ?? "Board-wide"}
                      {finding.regulatoryReference ? ` · ${finding.regulatoryReference}` : ""}
                    </p>
                  </div>
                ))}

                <form className="add-form" onSubmit={handleAddFinding}>
                  <label style={{ minWidth: "100%" }}>
                    Description
                    <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} required />
                  </label>
                  <label>
                    Severity
                    <select value={severity} onChange={(e) => setSeverity(e.target.value as FindingSeverity)}>
                      <option value="CRITICAL">Critical</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                      <option value="OBSERVATION">Observation</option>
                    </select>
                  </label>
                  <label>
                    Regulatory reference
                    <input value={regulatoryReference} onChange={(e) => setRegulatoryReference(e.target.value)} />
                  </label>
                  <label>
                    Evidence
                    <input value={evidence} onChange={(e) => setEvidence(e.target.value)} />
                  </label>
                  <label>
                    Root cause
                    <input value={rootCause} onChange={(e) => setRootCause(e.target.value)} />
                  </label>
                  <label>
                    Risk implication
                    <input value={riskImplication} onChange={(e) => setRiskImplication(e.target.value)} />
                  </label>
                  <button type="submit" disabled={addingFinding}>
                    {addingFinding ? "Adding..." : "Add finding"}
                  </button>
                </form>
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}
