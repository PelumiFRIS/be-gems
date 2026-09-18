import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { EvaluationDetail, ScoreRowSummary, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { getEvaluation } from "../api/evaluations";
import { calculateScores, getScores } from "../api/scores";
import { EvaluationResultsPage } from "./EvaluationResultsPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/evaluations", () => ({
  getEvaluation: vi.fn(),
}));

vi.mock("../api/scores", () => ({
  getScores: vi.fn(),
  calculateScores: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedGetEvaluation = vi.mocked(getEvaluation);
const mockedGetScores = vi.mocked(getScores);
const mockedCalculateScores = vi.mocked(calculateScores);

const user: UserSummary = {
  id: "user-1",
  firstName: "Cara",
  lastName: "Secretary",
  email: "cara@uitest.local",
  role: "COMPANY_SECRETARY",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
};

const EVAL_ID = "eval-1";

const boardScores: ScoreRowSummary[] = [
  {
    scopeType: "DIMENSION",
    dimensionId: "dim-1",
    dimensionName: "Board Composition",
    bgeiCategory: null,
    rawScore: 4,
    weightedScore: 0.4,
    maturityLevel: 4,
    maturityLabel: "Managed",
    bgeiBandLabel: null,
  },
  {
    scopeType: "BOARD_OVERALL",
    dimensionId: null,
    dimensionName: null,
    bgeiCategory: null,
    rawScore: 4,
    weightedScore: null,
    maturityLevel: 4,
    maturityLabel: "Managed",
    bgeiBandLabel: null,
  },
  {
    scopeType: "BGEI_CATEGORY",
    dimensionId: null,
    dimensionName: null,
    bgeiCategory: "Board Composition",
    rawScore: 4,
    weightedScore: 8,
    maturityLevel: null,
    maturityLabel: null,
    bgeiBandLabel: null,
  },
  {
    scopeType: "BGEI_OVERALL",
    dimensionId: null,
    dimensionName: null,
    bgeiCategory: null,
    rawScore: null,
    weightedScore: 80,
    maturityLevel: null,
    maturityLabel: null,
    bgeiBandLabel: "Highly Effective",
  },
];

const peerScores: ScoreRowSummary[] = [
  {
    scopeType: "DIMENSION",
    dimensionId: "dim-1",
    dimensionName: "Director Performance",
    bgeiCategory: null,
    rawScore: 3.5,
    weightedScore: null,
    maturityLevel: 3,
    maturityLabel: "Developing",
    bgeiBandLabel: null,
  },
  {
    scopeType: "DIRECTOR_OVERALL",
    dimensionId: null,
    dimensionName: null,
    bgeiCategory: null,
    rawScore: 3.5,
    weightedScore: null,
    maturityLevel: null,
    maturityLabel: null,
    bgeiBandLabel: null,
  },
];

function boardEvaluationDetail(status: EvaluationDetail["evaluation"]["status"]): EvaluationDetail {
  return {
    evaluation: {
      id: EVAL_ID,
      boardId: "board-1",
      evaluationType: "BOARD",
      subjectDirectorId: null,
      subjectDirectorName: null,
      year: 2026,
      status,
      startDate: "2026-01-01",
      closeDate: status === "CLOSED" || status === "SCORED" ? "2026-06-01" : null,
    },
    respondents: [],
  };
}

function peerEvaluationDetail(): EvaluationDetail {
  return {
    evaluation: {
      id: EVAL_ID,
      boardId: "board-1",
      evaluationType: "DIRECTOR_PEER",
      subjectDirectorId: "director-1",
      subjectDirectorName: "Ada Lovelace",
      year: 2026,
      status: "SCORED",
      startDate: "2026-01-01",
      closeDate: "2026-06-01",
    },
    respondents: [],
  };
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/evaluations/${EVAL_ID}/results`]}>
      <Routes>
        <Route path="/evaluations/:id/results" element={<EvaluationResultsPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EvaluationResultsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({ user, loading: false } as unknown as ReturnType<typeof useAuth>);
  });

  it("renders dimension scores, board overall and BGEI for a scored board evaluation", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);

    renderPage();

    expect(await screen.findByText(/Board Evaluation/)).toBeInTheDocument();
    expect(screen.getAllByText("Board Composition")).toHaveLength(2);
    expect(screen.getByText("Overall Board Score")).toBeInTheDocument();
    expect(screen.getByText(/4 \/ 5\.00/)).toBeInTheDocument();
    expect(screen.getByText("Board Governance Effectiveness Index")).toBeInTheDocument();
    expect(screen.getByText(/80.*Highly Effective/)).toBeInTheDocument();
  });

  it("renders the director overall score for a scored peer evaluation, without board-only sections", async () => {
    mockedGetEvaluation.mockResolvedValue(peerEvaluationDetail());
    mockedGetScores.mockResolvedValue(peerScores);

    renderPage();

    expect(await screen.findByText(/Peer Evaluation of Ada Lovelace/)).toBeInTheDocument();
    expect(screen.getByText("Director Performance")).toBeInTheDocument();
    expect(screen.getByText("Overall Director Score")).toBeInTheDocument();
    expect(screen.getByText(/3\.5 \/ 5\.00/)).toBeInTheDocument();
    expect(screen.queryByText("Overall Board Score")).not.toBeInTheDocument();
    expect(screen.queryByText("Board Governance Effectiveness Index")).not.toBeInTheDocument();
  });

  it("lets a closed-but-unscored evaluation trigger score calculation", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("CLOSED"));
    mockedGetScores.mockResolvedValue([]);
    mockedCalculateScores.mockResolvedValue(boardScores);

    renderPage();

    const calculateButton = await screen.findByRole("button", { name: "Calculate scores" });
    await userEvent.click(calculateButton);

    expect(mockedCalculateScores).toHaveBeenCalledWith(EVAL_ID);
    expect(await screen.findByText("Overall Board Score")).toBeInTheDocument();
  });

  it("shows a waiting message when the evaluation is not yet closed", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("LAUNCHED"));
    mockedGetScores.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("Scores are available once the evaluation is closed.")).toBeInTheDocument();
  });
});
