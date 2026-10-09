import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type {
  EvaluationDetail,
  FindingSummary,
  ReportApprovalStatus,
  ScoreRowSummary,
  UserSummary,
} from "../api/types";
import { approveReport, getReportApproval, returnReport } from "../api/reportApproval";
import { useAuth } from "../context/AuthContext";
import { getEvaluation } from "../api/evaluations";
import { createFinding, listFindings } from "../api/findings";
import { openEvaluationReport } from "../api/reports";
import { calculateScores, getScores } from "../api/scores";
import { getBenchmarkComparison } from "../api/benchmarks";
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

vi.mock("../api/findings", () => ({
  listFindings: vi.fn(),
  createFinding: vi.fn(),
}));

vi.mock("../api/reports", () => ({
  openEvaluationReport: vi.fn(),
}));

vi.mock("../api/benchmarks", () => ({
  getBenchmarkComparison: vi.fn(),
}));

vi.mock("../api/reportApproval", () => ({
  getReportApproval: vi.fn(),
  approveReport: vi.fn(),
  returnReport: vi.fn(),
}));

const mockedGetBenchmarkComparison = vi.mocked(getBenchmarkComparison);
const mockedGetReportApproval = vi.mocked(getReportApproval);
const mockedApproveReport = vi.mocked(approveReport);
const mockedReturnReport = vi.mocked(returnReport);

const STAGES: ReportApprovalStatus["stages"] = [
  { stage: "EVALUATOR_REVIEW", label: "Evaluator Review" },
  { stage: "DRAFT_REPORT", label: "Draft Report" },
  { stage: "QUALITY_REVIEW", label: "Quality Review" },
  { stage: "CS_REVIEW", label: "Company Secretary Review" },
  { stage: "BOARD_APPROVAL", label: "Chairman/Board Approval" },
  { stage: "FINAL", label: "Final Report" },
];

function approvalStatus(overrides: Partial<ReportApprovalStatus> = {}): ReportApprovalStatus {
  return {
    evaluationId: "eval-1",
    year: 2026,
    stage: "EVALUATOR_REVIEW",
    stageLabel: "Evaluator Review",
    stages: STAGES,
    history: [],
    canApprove: true,
    canReturn: false,
    commentRequired: false,
    approveLabel: "Results reviewed — prepare draft report",
    ...overrides,
  };
}

const mockedUseAuth = vi.mocked(useAuth);
const mockedGetEvaluation = vi.mocked(getEvaluation);
const mockedGetScores = vi.mocked(getScores);
const mockedCalculateScores = vi.mocked(calculateScores);
const mockedListFindings = vi.mocked(listFindings);
const mockedCreateFinding = vi.mocked(createFinding);
const mockedOpenEvaluationReport = vi.mocked(openEvaluationReport);

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
      reportStage: status === "SCORED" ? "EVALUATOR_REVIEW" : null,
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
      reportStage: null,
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
    mockedListFindings.mockResolvedValue([]);
    mockedGetBenchmarkComparison.mockResolvedValue({ rows: [], belowCount: 0 });
    mockedGetReportApproval.mockResolvedValue(approvalStatus());
  });

  it("moves the report on to the next approval stage", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedGetReportApproval.mockResolvedValue(
      approvalStatus({
        stage: "QUALITY_REVIEW",
        stageLabel: "Quality Review",
        canReturn: true,
        approveLabel: "Quality review passed — send to Company Secretary",
      }),
    );
    mockedApproveReport.mockResolvedValue(
      approvalStatus({
        stage: "CS_REVIEW",
        stageLabel: "Company Secretary Review",
        history: [
          {
            fromStage: "QUALITY_REVIEW",
            fromStageLabel: "Quality Review",
            toStage: "CS_REVIEW",
            toStageLabel: "Company Secretary Review",
            decision: "APPROVED",
            comment: null,
            actorName: "Cara Secretary",
            createdAt: "2026-10-09T10:00:00Z",
          },
        ],
        approveLabel: "Send to the Chairman/Board for approval",
      }),
    );

    renderPage();

    expect(await screen.findByText("The report is a draft at the Quality Review stage.")).toBeInTheDocument();
    expect(screen.getByText("Quality Review").closest("li")).toHaveClass("approval-step-current");
    expect(screen.getByText("Draft Report").closest("li")).toHaveClass("approval-step-done");
    await userEvent.click(screen.getByRole("button", { name: "Quality review passed — send to Company Secretary" }));

    expect(mockedApproveReport).toHaveBeenCalledWith(EVAL_ID, "");
    expect(await screen.findByText("The report is a draft at the Company Secretary Review stage.")).toBeInTheDocument();
    const historyRow = screen.getByRole("cell", { name: "Cara Secretary" }).closest("tr")!;
    expect(within(historyRow).getByText("Approved")).toBeInTheDocument();
  });

  it("requires a comment before returning the report for changes", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedGetReportApproval.mockResolvedValue(
      approvalStatus({ stage: "CS_REVIEW", stageLabel: "Company Secretary Review", canReturn: true }),
    );
    mockedReturnReport.mockResolvedValue(
      approvalStatus({ stage: "DRAFT_REPORT", stageLabel: "Draft Report", canReturn: true }),
    );

    renderPage();

    const returnButton = await screen.findByRole("button", { name: "Return to Draft Report" });
    expect(returnButton).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/Comment/), "Add the committee commentary");
    await userEvent.click(returnButton);

    expect(mockedReturnReport).toHaveBeenCalledWith(EVAL_ID, "Add the committee commentary");
    expect(await screen.findByText("The report is a draft at the Draft Report stage.")).toBeInTheDocument();
  });

  it("asks the Company Secretary to record when the Board approved the report", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedGetReportApproval.mockResolvedValue(
      approvalStatus({
        stage: "BOARD_APPROVAL",
        stageLabel: "Chairman/Board Approval",
        canReturn: true,
        commentRequired: true,
        approveLabel: "Record the Board's approval",
      }),
    );

    renderPage();

    const approve = await screen.findByRole("button", { name: "Record the Board's approval" });
    expect(approve).toBeDisabled();
    await userEvent.type(screen.getByLabelText(/Board approval record/), "Board meeting of 2 October 2026");
    expect(approve).toBeEnabled();
  });

  it("labels the report as final once it has been approved", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedGetReportApproval.mockResolvedValue(
      approvalStatus({ stage: "FINAL", stageLabel: "Final Report", canApprove: false, approveLabel: null }),
    );

    renderPage();

    expect(await screen.findByRole("button", { name: "Open final board report" })).toBeInTheDocument();
    expect(screen.getByText("The report has been approved and issued as final.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Return to/ })).not.toBeInTheDocument();
  });

  it("flags areas with a negative variance against their benchmark", async () => {
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedGetBenchmarkComparison.mockResolvedValue({
      rows: [
        {
          dimensionId: "dim-1",
          measure: "Strategy",
          actual: 4,
          benchmark: 4.2,
          variance: -0.2,
          belowBenchmark: true,
          source: "CBN Code 2023, s.5",
          defaultBenchmark: false,
        },
        {
          dimensionId: null,
          measure: "BGEI",
          actual: 80,
          benchmark: 70,
          variance: 10,
          belowBenchmark: false,
          source: "BE-GEMS default: the lower bound of the Effective band",
          defaultBenchmark: true,
        },
      ],
      belowCount: 1,
    });

    renderPage();

    expect(await screen.findByText("1 area falls below the benchmark.")).toBeInTheDocument();
    const strategy = screen.getByText("Strategy").closest("tr")!;
    expect(within(strategy).getByText("-0.20")).toHaveClass("variance-negative");
    expect(within(strategy).getByText("Below benchmark")).toBeInTheDocument();
    expect(within(strategy).getByText("CBN Code 2023, s.5")).toBeInTheDocument();
    const bgei = screen.getByText("BGEI").closest("tr")!;
    expect(within(bgei).getByText("+10.00%")).toBeInTheDocument();
    expect(within(bgei).getByText("Meets benchmark")).toBeInTheDocument();
    expect(mockedGetBenchmarkComparison).toHaveBeenCalledWith(EVAL_ID);
  });

  it("doesn't request a benchmark for a peer evaluation", async () => {
    mockedGetEvaluation.mockResolvedValue(peerEvaluationDetail());
    mockedGetScores.mockResolvedValue(peerScores);

    renderPage();

    expect(await screen.findByText("Overall Director Score")).toBeInTheDocument();
    expect(mockedGetBenchmarkComparison).not.toHaveBeenCalled();
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

  it("opens the board report in a new tab once scores exist", async () => {
    const tab = { document: { title: "", body: { textContent: "" } }, close: vi.fn() } as unknown as Window;
    const openSpy = vi.spyOn(window, "open").mockReturnValue(tab);
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedOpenEvaluationReport.mockResolvedValue();

    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Open draft board report" }));

    expect(openSpy).toHaveBeenCalledWith("", "_blank");
    expect(mockedOpenEvaluationReport).toHaveBeenCalledWith(EVAL_ID, tab);
    openSpy.mockRestore();
  });

  it("shows the server's message when the report can't be generated", async () => {
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedOpenEvaluationReport.mockRejectedValue(
      Object.assign(new Error("conflict"), {
        isAxiosError: true,
        response: { data: { message: "The report is available once scores have been calculated" } },
      }),
    );

    renderPage();
    await userEvent.click(await screen.findByRole("button", { name: "Open draft board report" }));

    expect(await screen.findByText("The report is available once scores have been calculated")).toBeInTheDocument();
    openSpy.mockRestore();
  });

  it("doesn't offer the confidential director report to an Organisation Administrator", async () => {
    mockedUseAuth.mockReturnValue({
      user: { ...user, role: "ORG_ADMIN" },
      loading: false,
    } as unknown as ReturnType<typeof useAuth>);
    mockedGetEvaluation.mockResolvedValue(peerEvaluationDetail());
    mockedGetScores.mockResolvedValue(peerScores);

    renderPage();

    expect(await screen.findByText("Overall Director Score")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /report/i })).not.toBeInTheDocument();
  });

  it("offers the confidential director report to an Organisation Administrator with Company Secretary access", async () => {
    mockedUseAuth.mockReturnValue({
      user: { ...user, role: "ORG_ADMIN", companySecretaryAccess: true },
      loading: false,
    } as unknown as ReturnType<typeof useAuth>);
    mockedGetEvaluation.mockResolvedValue(peerEvaluationDetail());
    mockedGetScores.mockResolvedValue(peerScores);

    renderPage();

    expect(await screen.findByRole("button", { name: "Open confidential director report" })).toBeInTheDocument();
  });

  it("offers the confidential director report to a Company Secretary", async () => {
    mockedGetEvaluation.mockResolvedValue(peerEvaluationDetail());
    mockedGetScores.mockResolvedValue(peerScores);

    renderPage();

    expect(await screen.findByRole("button", { name: "Open confidential director report" })).toBeInTheDocument();
  });

  it("lists existing findings and lets a Company Secretary record a new one", async () => {
    const existingFinding: FindingSummary = {
      id: "finding-1",
      evaluationId: EVAL_ID,
      dimensionId: "dim-1",
      dimensionName: "Board Composition",
      description: "Board packs circulated late",
      severity: "HIGH",
      evidence: null,
      regulatoryReference: null,
      rootCause: null,
      riskImplication: null,
      createdAt: "2026-01-01T00:00:00Z",
    };
    mockedGetEvaluation.mockResolvedValue(boardEvaluationDetail("SCORED"));
    mockedGetScores.mockResolvedValue(boardScores);
    mockedListFindings.mockResolvedValue([existingFinding]);
    mockedCreateFinding.mockResolvedValue({
      ...existingFinding,
      id: "finding-2",
      description: "Risk appetite not reviewed this year",
      severity: "MEDIUM",
    });

    renderPage();

    expect(await screen.findByText("Board packs circulated late")).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText("Description"), "Risk appetite not reviewed this year");
    await userEvent.selectOptions(screen.getByLabelText("Severity"), "MEDIUM");
    await userEvent.click(screen.getByRole("button", { name: "Add finding" }));

    expect(mockedCreateFinding).toHaveBeenCalledWith(EVAL_ID, {
      description: "Risk appetite not reviewed this year",
      severity: "MEDIUM",
      evidence: undefined,
      regulatoryReference: undefined,
      rootCause: undefined,
      riskImplication: undefined,
    });
    expect(await screen.findByText("Risk appetite not reviewed this year")).toBeInTheDocument();
  });
});
