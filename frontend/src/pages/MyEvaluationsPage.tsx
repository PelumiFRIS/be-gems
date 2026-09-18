import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { listMyEvaluations } from "../api/responses";
import type { MyEvaluationSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

export function MyEvaluationsPage() {
  const [evaluations, setEvaluations] = useState<MyEvaluationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listMyEvaluations()
      .then(setEvaluations)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>My Evaluations</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && (
          <section className="dashboard-section">
            <ul className="simple-list">
              {evaluations.map((evaluation) => (
                <li key={evaluation.evaluationId}>
                  {evaluation.myStatus === "SUBMITTED" ? (
                    <span>
                      {evaluation.evaluationType === "BOARD"
                        ? "Board Evaluation"
                        : `Peer Evaluation of ${evaluation.subjectDirectorName ?? "?"}`}{" "}
                      &mdash; {evaluation.year}
                    </span>
                  ) : (
                    <Link to={`/my-evaluations/${evaluation.evaluationId}`}>
                      {evaluation.evaluationType === "BOARD"
                        ? "Board Evaluation"
                        : `Peer Evaluation of ${evaluation.subjectDirectorName ?? "?"}`}{" "}
                      &mdash; {evaluation.year}
                    </Link>
                  )}
                  <span className="table-hint">
                    {evaluation.myStatus} &middot; {evaluation.answeredQuestions}/{evaluation.totalQuestions} answered
                  </span>
                </li>
              ))}
              {evaluations.length === 0 && <li>You have no evaluations to complete.</li>}
            </ul>
          </section>
        )}
      </main>
    </div>
  );
}
