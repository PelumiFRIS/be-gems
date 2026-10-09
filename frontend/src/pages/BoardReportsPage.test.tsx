import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { approveReport, getReportApproval, listMyBoardReports } from "../api/reportApproval";
import { openEvaluationReport } from "../api/reports";
import type { ReportApprovalStatus, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { BoardReportsPage } from "./BoardReportsPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/reportApproval", () => ({
  listMyBoardReports: vi.fn(),
  getReportApproval: vi.fn(),
  approveReport: vi.fn(),
  returnReport: vi.fn(),
}));

vi.mock("../api/reports", () => ({
  openEvaluationReport: vi.fn(),
}));

vi.mock("../components/TopBar", () => ({
  TopBar: () => null,
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedListMyBoardReports = vi.mocked(listMyBoardReports);
const mockedGetReportApproval = vi.mocked(getReportApproval);
const mockedApproveReport = vi.mocked(approveReport);
const mockedOpenEvaluationReport = vi.mocked(openEvaluationReport);

const chairman: UserSummary = {
  id: "user-chair",
  firstName: "Chidi",
  lastName: "Chairman",
  email: "chidi@uitest.local",
  role: "DIRECTOR",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
};

const atBoardApproval: ReportApprovalStatus = {
  evaluationId: "eval-1",
  year: 2026,
  stage: "BOARD_APPROVAL",
  stageLabel: "Chairman/Board Approval",
  stages: [
    { stage: "EVALUATOR_REVIEW", label: "Evaluator Review" },
    { stage: "DRAFT_REPORT", label: "Draft Report" },
    { stage: "QUALITY_REVIEW", label: "Quality Review" },
    { stage: "CS_REVIEW", label: "Company Secretary Review" },
    { stage: "BOARD_APPROVAL", label: "Chairman/Board Approval" },
    { stage: "FINAL", label: "Final Report" },
  ],
  history: [],
  canApprove: true,
  canReturn: true,
  commentRequired: false,
  approveLabel: "Approve the final report",
};

function renderPage() {
  return render(
    <MemoryRouter>
      <BoardReportsPage />
    </MemoryRouter>,
  );
}

describe("BoardReportsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({ user: chairman, loading: false } as unknown as ReturnType<typeof useAuth>);
  });

  it("lets the Chairman approve the report and then open the final version", async () => {
    mockedListMyBoardReports.mockResolvedValue([
      {
        evaluationId: "eval-1",
        year: 2026,
        boardName: "Board of Directors",
        stage: "BOARD_APPROVAL",
        stageLabel: "Chairman/Board Approval",
        awaitingMyApproval: true,
      },
    ]);
    mockedGetReportApproval.mockResolvedValue(atBoardApproval);
    mockedApproveReport.mockResolvedValue({
      ...atBoardApproval,
      stage: "FINAL",
      stageLabel: "Final Report",
      canApprove: false,
      canReturn: false,
      approveLabel: null,
    });
    mockedOpenEvaluationReport.mockResolvedValue();
    const openSpy = vi.spyOn(window, "open").mockReturnValue(null);

    renderPage();

    expect(await screen.findByText("Board Evaluation Report — 2026")).toBeInTheDocument();
    expect(screen.getByText("Draft · Chairman/Board Approval")).toBeInTheDocument();
    await userEvent.click(await screen.findByRole("button", { name: "Approve the final report" }));

    expect(mockedApproveReport).toHaveBeenCalledWith("eval-1", "");
    expect(await screen.findByText("The report has been approved and issued as final.")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Open final report" }));
    expect(mockedOpenEvaluationReport).toHaveBeenCalledWith("eval-1", null);
    openSpy.mockRestore();
  });

  it("shows the final report to other directors without approval controls", async () => {
    mockedListMyBoardReports.mockResolvedValue([
      {
        evaluationId: "eval-1",
        year: 2026,
        boardName: "Board of Directors",
        stage: "FINAL",
        stageLabel: "Final Report",
        awaitingMyApproval: false,
      },
    ]);

    renderPage();

    expect(await screen.findByRole("button", { name: "Open final report" })).toBeInTheDocument();
    expect(screen.getByText("Final")).toBeInTheDocument();
    expect(mockedGetReportApproval).not.toHaveBeenCalled();
  });

  it("explains when no report is available yet", async () => {
    mockedListMyBoardReports.mockResolvedValue([]);

    renderPage();

    expect(
      await screen.findByText("The Board Evaluation Report will appear here once it has been approved."),
    ).toBeInTheDocument();
  });
});
