import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { listDirectors } from "../api/directors";
import { createEvaluation, listEvaluations } from "../api/evaluations";
import type { BoardSummary, DirectorSummary, EvaluationSummary, EvaluationType } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

export function EvaluationsListPage() {
  const [board, setBoard] = useState<BoardSummary | null>(null);
  const [directors, setDirectors] = useState<DirectorSummary[]>([]);
  const [evaluations, setEvaluations] = useState<EvaluationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [evaluationType, setEvaluationType] = useState<EvaluationType>("BOARD");
  const [subjectDirectorId, setSubjectDirectorId] = useState("");
  const [year, setYear] = useState(new Date().getFullYear());
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    listBoards()
      .then(async (boards) => {
        if (boards.length === 0) {
          setLoading(false);
          return;
        }
        setBoard(boards[0]);
        const [directorList, evaluationList] = await Promise.all([
          listDirectors(boards[0].id),
          listEvaluations(boards[0].id),
        ]);
        setDirectors(directorList);
        setEvaluations(evaluationList);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    if (!board) return;
    setError(null);
    setCreating(true);
    try {
      const created = await createEvaluation({
        boardId: board.id,
        evaluationType,
        subjectDirectorId: evaluationType === "DIRECTOR_PEER" ? subjectDirectorId : undefined,
        year,
      });
      setEvaluations((prev) => [created, ...prev]);
      setSubjectDirectorId("");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Evaluations</h1>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && !board && <p>Set up your board first on the Board Setup page.</p>}

        {board && (
          <>
            <section className="dashboard-section">
              <ul className="simple-list">
                {evaluations.map((evaluation) => (
                  <li key={evaluation.id}>
                    <Link to={`/evaluations/${evaluation.id}`}>
                      {evaluation.evaluationType === "BOARD"
                        ? "Board Evaluation"
                        : `Peer Evaluation of ${evaluation.subjectDirectorName ?? "?"}`}{" "}
                      &mdash; {evaluation.year}
                    </Link>
                    <span className="table-hint">{evaluation.status}</span>
                  </li>
                ))}
                {evaluations.length === 0 && <li>No evaluations yet.</li>}
              </ul>
            </section>

            <section className="dashboard-section">
              <h2>Create an evaluation</h2>
              <form className="add-form" onSubmit={handleCreate}>
                <label>
                  Type
                  <select value={evaluationType} onChange={(e) => setEvaluationType(e.target.value as EvaluationType)}>
                    <option value="BOARD">Board</option>
                    <option value="DIRECTOR_PEER">Director Peer-to-Peer</option>
                  </select>
                </label>
                {evaluationType === "DIRECTOR_PEER" && (
                  <label>
                    Subject director
                    <select value={subjectDirectorId} onChange={(e) => setSubjectDirectorId(e.target.value)} required>
                      <option value="" disabled>
                        Choose a director...
                      </option>
                      {directors.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                <label>
                  Year
                  <input
                    type="number"
                    value={year}
                    onChange={(e) => setYear(Number(e.target.value))}
                    min={2000}
                    required
                  />
                </label>
                <button type="submit" disabled={creating}>
                  {creating ? "Creating..." : "Create evaluation"}
                </button>
              </form>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
