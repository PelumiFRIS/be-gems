import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type {
  ActionRegisterRow,
  BoardSummary,
  DirectorSummary,
  EvaluationSummary,
  FindingSummary,
  ScoreRowSummary,
} from "../api/types";
import { useAuth } from "../context/AuthContext";
import { listBoards } from "../api/boards";
import { listDirectors } from "../api/directors";
import { listEvaluations } from "../api/evaluations";
import { listFindings } from "../api/findings";
import { getActionRegister } from "../api/actions";
import { getScores } from "../api/scores";
import { DashboardPage } from "./DashboardPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/boards", () => ({
  listBoards: vi.fn(),
}));

vi.mock("../api/directors", () => ({
  listDirectors: vi.fn(),
}));

vi.mock("../api/evaluations", () => ({
  listEvaluations: vi.fn(),
}));

vi.mock("../api/findings", () => ({
  listFindings: vi.fn(),
}));

vi.mock("../api/actions", () => ({
  getActionRegister: vi.fn(),
}));

vi.mock("../api/scores", () => ({
  getScores: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedListBoards = vi.mocked(listBoards);
const mockedListDirectors = vi.mocked(listDirectors);
const mockedListEvaluations = vi.mocked(listEvaluations);
const mockedListFindings = vi.mocked(listFindings);
const mockedGetActionRegister = vi.mocked(getActionRegister);
const mockedGetScores = vi.mocked(getScores);

const board: BoardSummary = { id: "board-1", name: "Main Board", effectiveDate: "2024-01-01", notes: null };

const directors: DirectorSummary[] = [
  {
    id: "dir-1",
    boardId: board.id,
    name: "Ada Lovelace",
    email: "ada@example.com",
    classification: "INDEPENDENT_NON_EXECUTIVE",
    appointmentDate: "2024-01-01",
    termExpirationDate: null,
    hasPortalAccess: true,
  },
];

const evaluations: EvaluationSummary[] = [
  {
    id: "eval-1",
    boardId: board.id,
    evaluationType: "BOARD",
    subjectDirectorId: null,
    subjectDirectorName: null,
    year: 2026,
    status: "SCORED",
    startDate: "2026-01-01",
    closeDate: "2026-06-01",
  },
  {
    id: "eval-2",
    boardId: board.id,
    evaluationType: "DIRECTOR_PEER",
    subjectDirectorId: "dir-1",
    subjectDirectorName: "Ada Lovelace",
    year: 2026,
    status: "DRAFT",
    startDate: null,
    closeDate: null,
  },
];

const findings: FindingSummary[] = [
  {
    id: "finding-1",
    evaluationId: "eval-1",
    dimensionId: null,
    dimensionName: null,
    description: "Board packs circulated late",
    severity: "HIGH",
    evidence: null,
    regulatoryReference: null,
    rootCause: null,
    riskImplication: null,
    createdAt: "2026-01-01T00:00:00Z",
  },
];

const actionRegister: ActionRegisterRow[] = [
  {
    id: "action-1",
    findingId: "finding-1",
    description: "Circulate packs 5 days in advance",
    owner: "Company Secretary",
    approver: null,
    dueDate: "2026-01-01",
    status: "NOT_STARTED",
    evidence: null,
    closureDate: null,
    overdue: true,
    evaluationId: "eval-1",
    evaluationYear: 2026,
    evaluationType: "BOARD",
    findingDescription: "Board packs circulated late",
    findingSeverity: "HIGH",
  },
];

const bgeiScores: ScoreRowSummary[] = [
  {
    scopeType: "BGEI_OVERALL",
    dimensionId: null,
    dimensionName: null,
    bgeiCategory: null,
    rawScore: null,
    weightedScore: 82.5,
    maturityLevel: null,
    maturityLabel: null,
    bgeiBandLabel: "Highly Effective",
  },
];

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <Routes>
        <Route path="/dashboard" element={<DashboardPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({
      user: {
        id: "user-1",
        firstName: "Cara",
        lastName: "Secretary",
        email: "cara@example.com",
        role: "COMPANY_SECRETARY",
        status: "ACTIVE",
        organizationId: "org-1",
        organizationName: "Local UI Test Org",
      },
      loading: false,
    } as unknown as ReturnType<typeof useAuth>);
  });

  it("shows an empty state prompting board setup when no board exists", async () => {
    mockedListBoards.mockResolvedValue([]);

    renderDashboard();

    expect(await screen.findByText("Set up your board to get started")).toBeInTheDocument();
  });

  it("shows KPI tiles, the BGEI score and recent evaluations for an existing board", async () => {
    mockedListBoards.mockResolvedValue([board]);
    mockedListDirectors.mockResolvedValue(directors);
    mockedListEvaluations.mockResolvedValue(evaluations);
    mockedGetActionRegister.mockResolvedValue(actionRegister);
    mockedListFindings.mockImplementation((evaluationId) =>
      Promise.resolve(evaluationId === "eval-1" ? findings : []),
    );
    mockedGetScores.mockResolvedValue(bgeiScores);

    renderDashboard();

    expect(await screen.findByText("82.5%")).toBeInTheDocument();
    expect(screen.getByText("Highly Effective")).toBeInTheDocument();
    expect(screen.getByText("Directors on the board")).toBeInTheDocument();
    expect(screen.getByText("Board Evaluation")).toBeInTheDocument();
    expect(screen.getByText("Peer Evaluation of Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByText(/Overdue action of 1 total/)).toBeInTheDocument();
  });
});
