import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AuthProvider } from "./context/AuthContext";
import { ActionRegisterPage } from "./pages/ActionRegisterPage";
import { BoardSetupPage } from "./pages/BoardSetupPage";
import { DashboardPage } from "./pages/DashboardPage";
import { EvaluationResultsPage } from "./pages/EvaluationResultsPage";
import { EvaluationSetupPage } from "./pages/EvaluationSetupPage";
import { EvaluationsListPage } from "./pages/EvaluationsListPage";
import { FindingDetailPage } from "./pages/FindingDetailPage";
import { LoginPage } from "./pages/LoginPage";
import { MyEvaluationsPage } from "./pages/MyEvaluationsPage";
import { RespondentQuestionnairePage } from "./pages/RespondentQuestionnairePage";
import { SignupPage } from "./pages/SignupPage";

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/board-setup"
          element={
            <ProtectedRoute>
              <BoardSetupPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/evaluations"
          element={
            <ProtectedRoute>
              <EvaluationsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/evaluations/:id"
          element={
            <ProtectedRoute>
              <EvaluationSetupPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/evaluations/:id/results"
          element={
            <ProtectedRoute>
              <EvaluationResultsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/findings/:findingId"
          element={
            <ProtectedRoute>
              <FindingDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/actions"
          element={
            <ProtectedRoute>
              <ActionRegisterPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-evaluations"
          element={
            <ProtectedRoute>
              <MyEvaluationsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/my-evaluations/:id"
          element={
            <ProtectedRoute>
              <RespondentQuestionnairePage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  );
}

export default App;
