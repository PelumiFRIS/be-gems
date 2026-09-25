import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { listDirectors } from "../api/directors";
import { listEvaluations } from "../api/evaluations";
import { listFindings } from "../api/findings";
import { getActionRegister } from "../api/actions";
import { getScores } from "../api/scores";
import type { ActionRegisterRow, BoardSummary, DirectorSummary, EvaluationSummary, FindingSummary } from "../api/types";
import { Badge } from "../components/Badge";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { ActionsIcon, BoardIcon, ChevronRightIcon, DirectorsIcon, FindingsIcon } from "../components/icons";
import { useAuth } from "../context/AuthContext";

interface BgeiSnapshot {
  score: number;
  band: string | null;
  year: number;
}

export function DashboardPage() {
  const { user } = useAuth();
  const [board, setBoard] = useState<BoardSummary | null>(null);
  const [directors, setDirectors] = useState<DirectorSummary[]>([]);
  const [evaluations, setEvaluations] = useState<EvaluationSummary[]>([]);
  const [findings, setFindings] = useState<FindingSummary[]>([]);
  const [actionRegister, setActionRegister] = useState<ActionRegisterRow[]>([]);
  const [bgei, setBgei] = useState<BgeiSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listBoards()
      .then(async (boards) => {
        if (boards.length === 0) {
          setLoading(false);
          return;
        }
        const activeBoard = boards[0];
        setBoard(activeBoard);

        const [directorList, evaluationList, register] = await Promise.all([
          listDirectors(activeBoard.id),
          listEvaluations(activeBoard.id),
          getActionRegister(),
        ]);
        setDirectors(directorList);
        setEvaluations(evaluationList);
        setActionRegister(register);

        const findingLists = await Promise.all(evaluationList.map((e) => listFindings(e.id)));
        setFindings(findingLists.flat());

        const latestScoredBoard = evaluationList
          .filter((e) => e.evaluationType === "BOARD" && e.status === "SCORED")
          .sort((a, b) => b.year - a.year)[0];
        if (latestScoredBoard) {
          const scores = await getScores(latestScoredBoard.id);
          const overall = scores.find((s) => s.scopeType === "BGEI_OVERALL");
          if (overall?.weightedScore != null) {
            setBgei({ score: overall.weightedScore, band: overall.bgeiBandLabel, year: latestScoredBoard.year });
          }
        }
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (!user) return null;

  const overdueActions = actionRegister.filter((a) => a.overdue).length;
  const severityCounts = ["CRITICAL", "HIGH", "MEDIUM", "LOW", "OBSERVATION"] as const;
  const evaluationStatusCounts = ["DRAFT", "LAUNCHED", "CLOSED", "SCORED"] as const;
  const recentEvaluations = [...evaluations].sort((a, b) => b.year - a.year).slice(0, 5);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Welcome, {user.firstName}</h1>
          <p>{user.organizationName}</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && !board && (
          <section className="dashboard-section empty-state">
            <h2>Set up your board to get started</h2>
            <p>Add your board, its directors and committees before launching an evaluation cycle.</p>
            <Link to="/board-setup">
              <button type="button">Go to Board Setup</button>
            </Link>
          </section>
        )}

        {!loading && board && (
          <>
            {bgei && (
              <div className="bgei-card">
                <div>
                  <p className="bgei-card-label">Board Governance Effectiveness Index &mdash; {bgei.year}</p>
                  <div className="bgei-card-score">{bgei.score.toFixed(1)}%</div>
                  {bgei.band && <div className="bgei-card-band">{bgei.band}</div>}
                </div>
                <p className="bgei-card-meta">Based on the most recently scored board evaluation.</p>
              </div>
            )}

            <div className="stat-grid">
              <div className="stat-card">
                <div className="stat-card-top">
                  <span className="stat-card-icon tone-primary">
                    <DirectorsIcon width={18} height={18} />
                  </span>
                </div>
                <div>
                  <div className="stat-card-value">{directors.length}</div>
                  <div className="stat-card-label">Directors on the board</div>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-card-top">
                  <span className="stat-card-icon tone-primary">
                    <BoardIcon width={18} height={18} />
                  </span>
                </div>
                <div>
                  <div className="stat-card-value">{evaluations.length}</div>
                  <div className="stat-card-label">Evaluations run</div>
                </div>
                <div className="stat-card-breakdown">
                  {evaluationStatusCounts.map((status) => {
                    const count = evaluations.filter((e) => e.status === status).length;
                    return count > 0 ? (
                      <Badge key={status} value={status} label={`${count} ${status.replace(/_/g, " ")}`} />
                    ) : null;
                  })}
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-card-top">
                  <span className="stat-card-icon tone-accent">
                    <FindingsIcon width={18} height={18} />
                  </span>
                </div>
                <div>
                  <div className="stat-card-value">{findings.length}</div>
                  <div className="stat-card-label">Findings recorded</div>
                </div>
                <div className="stat-card-breakdown">
                  {severityCounts.map((severity) => {
                    const count = findings.filter((f) => f.severity === severity).length;
                    return count > 0 ? (
                      <Badge key={severity} value={severity} label={`${count} ${severity}`} />
                    ) : null;
                  })}
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-card-top">
                  <span className={`stat-card-icon ${overdueActions > 0 ? "tone-danger" : "tone-success"}`}>
                    <ActionsIcon width={18} height={18} />
                  </span>
                </div>
                <div>
                  <div className="stat-card-value">{overdueActions}</div>
                  <div className="stat-card-label">
                    Overdue action{overdueActions === 1 ? "" : "s"} of {actionRegister.length} total
                  </div>
                </div>
                <Link to="/actions" className="stat-card-link">
                  View action register <ChevronRightIcon width={14} height={14} />
                </Link>
              </div>
            </div>

            <section className="dashboard-section">
              <div className="section-heading-row">
                <h2>Recent evaluations</h2>
                <Link to="/evaluations" className="stat-card-link">
                  View all <ChevronRightIcon width={14} height={14} />
                </Link>
              </div>

              {recentEvaluations.length === 0 && (
                <p className="table-hint">No evaluations yet &mdash; start one from the Evaluations page.</p>
              )}

              {recentEvaluations.length > 0 && (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Evaluation</th>
                      <th>Year</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentEvaluations.map((evaluation) => (
                      <tr key={evaluation.id}>
                        <td>
                          <Link to={`/evaluations/${evaluation.id}`}>
                            {evaluation.evaluationType === "BOARD"
                              ? "Board Evaluation"
                              : `Peer Evaluation of ${evaluation.subjectDirectorName ?? "?"}`}
                          </Link>
                        </td>
                        <td>{evaluation.year}</td>
                        <td>
                          <Badge value={evaluation.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
