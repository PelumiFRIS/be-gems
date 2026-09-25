import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getActionRegister } from "../api/actions";
import { extractErrorMessage } from "../api/client";
import type { ActionRegisterRow } from "../api/types";
import { Badge } from "../components/Badge";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

export function ActionRegisterPage() {
  const [rows, setRows] = useState<ActionRegisterRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getActionRegister()
      .then(setRows)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const overdueCount = rows.filter((r) => r.overdue).length;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Action Register</h1>
          <p>Every corrective action across your organisation&rsquo;s evaluations, in one place.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading && (
          <section className="dashboard-section">
            <h2>
              {rows.length} action{rows.length === 1 ? "" : "s"}
              {overdueCount > 0 && <> &mdash; {overdueCount} overdue</>}
            </h2>

            {rows.length === 0 && <p className="table-hint">No corrective actions have been recorded yet.</p>}

            {rows.length > 0 && (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Finding</th>
                    <th>Evaluation</th>
                    <th>Owner</th>
                    <th>Due date</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id} className={row.overdue ? "overdue" : undefined}>
                      <td>{row.description}</td>
                      <td>
                        <Link to={`/findings/${row.findingId}`}>{row.findingDescription ?? "View finding"}</Link>
                        {row.findingSeverity && (
                          <>
                            {" "}
                            <Badge value={row.findingSeverity} />
                          </>
                        )}
                      </td>
                      <td>
                        {row.evaluationType === "BOARD" ? "Board" : "Peer"} evaluation &mdash; {row.evaluationYear}
                      </td>
                      <td>{row.owner ?? "—"}</td>
                      <td>
                        {row.dueDate ?? "—"} {row.overdue && <Badge value="OVERDUE" />}
                      </td>
                      <td>
                        <Badge value={row.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        )}
      </main>
    </div>
  );
}
