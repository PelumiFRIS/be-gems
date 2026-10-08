import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listDirectors } from "../api/directors";
import { getEvaluation, sendEvaluationReminders } from "../api/evaluations";
import type { EvaluationDetail, RespondentStatus } from "../api/types";
import { EvaluationSetupPage } from "./EvaluationSetupPage";

vi.mock("../components/Sidebar", () => ({ Sidebar: () => null }));
vi.mock("../components/TopBar", () => ({ TopBar: () => null }));
vi.mock("../context/AuthContext", () => ({
  useAuth: () => ({ user: { role: "ORG_ADMIN", companySecretaryAccess: true } }),
}));

vi.mock("../api/directors", () => ({
  listDirectors: vi.fn(),
}));

vi.mock("../api/evaluations", () => ({
  addRespondent: vi.fn(),
  closeEvaluation: vi.fn(),
  getEvaluation: vi.fn(),
  launchEvaluation: vi.fn(),
  sendEvaluationReminders: vi.fn(),
}));

const mockedGetEvaluation = vi.mocked(getEvaluation);
const mockedListDirectors = vi.mocked(listDirectors);
const mockedSendReminders = vi.mocked(sendEvaluationReminders);

function launchedEvaluation(statuses: RespondentStatus[]): EvaluationDetail {
  return {
    evaluation: {
      id: "eval-1",
      boardId: "board-1",
      evaluationType: "BOARD",
      subjectDirectorId: null,
      subjectDirectorName: null,
      year: 2026,
      status: "LAUNCHED",
      startDate: "2026-10-01",
      closeDate: null,
    },
    respondents: statuses.map((status, i) => ({
      id: `r-${i}`,
      directorId: `d-${i}`,
      directorName: `Director ${i}`,
      confidentialityMode: "CONFIDENTIAL",
      status,
      submittedAt: status === "SUBMITTED" ? "2026-10-02T09:00:00Z" : null,
    })),
  };
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/evaluations/eval-1"]}>
      <Routes>
        <Route path="/evaluations/:id" element={<EvaluationSetupPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("EvaluationSetupPage reminders", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedListDirectors.mockResolvedValue([]);
  });

  it("reminds outstanding respondents and reports anyone who couldn't be reached", async () => {
    mockedGetEvaluation.mockResolvedValue(launchedEvaluation(["SUBMITTED", "IN_PROGRESS", "INVITED"]));
    mockedSendReminders.mockResolvedValue({ remindersSent: 1, withoutPortalAccess: 1 });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Send reminder to 2 outstanding respondents" }));

    expect(mockedSendReminders).toHaveBeenCalledWith("eval-1");
    expect(
      await screen.findByText(
        "Reminder sent to 1 respondent. 1 outstanding respondent hasn't been invited to the portal yet, so couldn't be reminded.",
      ),
    ).toBeInTheDocument();
  });

  it("hides the reminder button once everyone has submitted", async () => {
    mockedGetEvaluation.mockResolvedValue(launchedEvaluation(["SUBMITTED", "SUBMITTED"]));
    renderPage();

    expect(await screen.findByRole("button", { name: "Close evaluation" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Send reminder/ })).not.toBeInTheDocument();
  });
});
