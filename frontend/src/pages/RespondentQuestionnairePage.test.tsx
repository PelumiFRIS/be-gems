import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { getFramework } from "../api/frameworks";
import { getMyQuestions, saveMyResponse, submitMyEvaluation } from "../api/responses";
import { RespondentQuestionnairePage } from "./RespondentQuestionnairePage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/frameworks", () => ({
  getFramework: vi.fn(),
}));

vi.mock("../api/responses", () => ({
  getMyQuestions: vi.fn(),
  saveMyResponse: vi.fn(),
  submitMyEvaluation: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedGetFramework = vi.mocked(getFramework);
const mockedGetMyQuestions = vi.mocked(getMyQuestions);
const mockedSaveMyResponse = vi.mocked(saveMyResponse);
const mockedSubmitMyEvaluation = vi.mocked(submitMyEvaluation);

const user: UserSummary = {
  id: "user-1",
  firstName: "Grace",
  lastName: "Hopper",
  email: "grace@uitest.local",
  role: "DIRECTOR",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
};

const EVAL_ID = "eval-1";

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/my-evaluations/${EVAL_ID}`]}>
      <Routes>
        <Route path="/my-evaluations/:id" element={<RespondentQuestionnairePage />} />
        <Route path="/my-evaluations" element={<div>My evaluations list</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("RespondentQuestionnairePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({ user, loading: false } as unknown as ReturnType<typeof useAuth>);
    mockedGetFramework.mockResolvedValue({
      id: "framework-1",
      code: "NCCG_2018",
      name: "NCCG 2018",
      version: "1.0",
      dimensions: [{ id: "dim-1", code: "BC", name: "Board Composition", defaultWeightPct: 10, bgeiCategory: "Board Composition" }],
    });
  });

  it("shows a previously saved answer and lets the respondent resume", async () => {
    mockedGetMyQuestions.mockResolvedValue([
      {
        questionId: "q-1",
        dimensionId: "dim-1",
        text: "The board meets its stated quorum requirements.",
        responseType: "RATING_1_5",
        mandatory: true,
        ratingValue: 4,
        textValue: null,
        numericValue: null,
      },
    ]);

    renderPage();

    expect(await screen.findByText("Board Composition")).toBeInTheDocument();
    expect(screen.getByText("1/1 answered")).toBeInTheDocument();

    const savedButton = screen.getByRole("button", { name: "4" });
    expect(savedButton).not.toHaveClass("secondary");

    const otherButton = screen.getByRole("button", { name: "5" });
    expect(otherButton).toHaveClass("secondary");
  });

  it("saves a new rating and submits the evaluation", async () => {
    mockedGetMyQuestions.mockResolvedValue([
      {
        questionId: "q-1",
        dimensionId: "dim-1",
        text: "The board meets its stated quorum requirements.",
        responseType: "RATING_1_5",
        mandatory: true,
        ratingValue: null,
        textValue: null,
        numericValue: null,
      },
    ]);
    mockedSaveMyResponse.mockResolvedValue({
      questionId: "q-1",
      dimensionId: "dim-1",
      text: "The board meets its stated quorum requirements.",
      responseType: "RATING_1_5",
      mandatory: true,
      ratingValue: 5,
      textValue: null,
      numericValue: null,
    });
    mockedSubmitMyEvaluation.mockResolvedValue(undefined);

    renderPage();

    expect(await screen.findByText("0/1 answered")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "5" }));

    await waitFor(() => {
      expect(mockedSaveMyResponse).toHaveBeenCalledWith(EVAL_ID, "q-1", { ratingValue: 5 });
    });
    expect(await screen.findByText("1/1 answered")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Submit evaluation" }));

    await waitFor(() => {
      expect(mockedSubmitMyEvaluation).toHaveBeenCalledWith(EVAL_ID);
    });
    expect(await screen.findByText("My evaluations list")).toBeInTheDocument();
  });
});
