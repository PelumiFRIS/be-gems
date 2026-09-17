import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { useAuth } from "../context/AuthContext";

export function DashboardPage() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Welcome, {user.firstName}</h1>
          <p>{user.organizationName}</p>
        </div>
        <section className="dashboard-section">
          <p>
            This is the BE-GEMS scaffold. Set up your board, directors, and committees on the{" "}
            <strong>Board Setup</strong> page — the evaluation cycle (questionnaires, scoring,
            governance maturity, findings, and reports) is built module by module in the sessions
            that follow this one.
          </p>
        </section>
      </main>
    </div>
  );
}
