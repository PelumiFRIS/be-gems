import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listBoards } from "../api/boards";
import { addSkill, getSkillsMatrix, rateSkill, removeSkill } from "../api/skills";
import type { SkillRow, SkillsMatrix, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { SkillsMatrixPage } from "./SkillsMatrixPage";

vi.mock("../components/Sidebar", () => ({ Sidebar: () => null }));
vi.mock("../components/TopBar", () => ({ TopBar: () => null }));
vi.mock("../context/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../api/boards", () => ({ listBoards: vi.fn() }));
vi.mock("../api/skills", () => ({
  getSkillsMatrix: vi.fn(),
  addSkill: vi.fn(),
  updateSkill: vi.fn(),
  removeSkill: vi.fn(),
  rateSkill: vi.fn(),
}));

function userWithRole(role: UserSummary["role"]): UserSummary {
  return {
    id: "u-1",
    firstName: "Modupeola",
    lastName: "Akanbi",
    email: "m@example.com",
    role,
    status: "ACTIVE",
    organizationId: "org-1",
    organizationName: "Acme",
    companySecretaryAccess: false,
  } as UserSummary;
}

function skill(overrides: Partial<SkillRow>): SkillRow {
  return {
    id: "s-1",
    name: "Strategy",
    requiredLevel: "HIGH",
    futureFocus: false,
    displayOrder: 1,
    ratings: {},
    average: null,
    proficientCount: 0,
    proficientDirectors: [],
    coverage: "NOT_ASSESSED",
    ...overrides,
  };
}

const matrix: SkillsMatrix = {
  boardId: "board-1",
  directors: [
    { id: "d-1", name: "Chidi Chairman", classification: "CHAIRMAN" },
    { id: "d-2", name: "Ngozi Member", classification: "NON_EXECUTIVE_DIRECTOR" },
  ],
  skills: [
    skill({
      id: "s-1",
      name: "Strategy",
      ratings: { "d-1": 5, "d-2": 3 },
      average: 4,
      proficientCount: 1,
      proficientDirectors: ["Chidi Chairman"],
      coverage: "SINGLE_PERSON_DEPENDENCY",
    }),
    skill({ id: "s-2", name: "Audit", displayOrder: 2, ratings: { "d-2": 2 }, average: 2, coverage: "CRITICAL_GAP" }),
    skill({ id: "s-3", name: "Cybersecurity", requiredLevel: "MEDIUM", futureFocus: true, displayOrder: 3 }),
  ],
  coverageCounts: { SINGLE_PERSON_DEPENDENCY: 1, CRITICAL_GAP: 1, NOT_ASSESSED: 1 },
};

function renderPage() {
  return render(
    <MemoryRouter>
      <SkillsMatrixPage />
    </MemoryRouter>,
  );
}

describe("SkillsMatrixPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listBoards).mockResolvedValue([{ id: "board-1", name: "Board of Directors" }] as Awaited<
      ReturnType<typeof listBoards>
    >);
    vi.mocked(getSkillsMatrix).mockResolvedValue(matrix);
  });

  it("explains each gap and lists future skills", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("COMPANY_SECRETARY") } as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByText(/only Chidi Chairman is rated Advanced or above/)).toBeInTheDocument();
    expect(screen.getByText(/High requirement, but no director is rated Advanced or above/)).toBeInTheDocument();
    expect(screen.getByText(/1 of 3 competencies have no ratings yet/)).toBeInTheDocument();
    const future = screen.getByRole("heading", { name: "Future skills requirements" }).closest("section")!;
    expect(within(future).getByText("Cybersecurity")).toBeInTheDocument();
  });

  it("saves a rating change and shows the recalculated matrix", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("COMPANY_SECRETARY") } as ReturnType<typeof useAuth>);
    vi.mocked(rateSkill).mockResolvedValue({
      ...matrix,
      skills: [
        { ...matrix.skills[0], ratings: { "d-1": 5, "d-2": 4 }, proficientCount: 2, coverage: "COVERED" },
        ...matrix.skills.slice(1),
      ],
      coverageCounts: { COVERED: 1, CRITICAL_GAP: 1, NOT_ASSESSED: 1 },
    });
    const user = userEvent.setup();
    renderPage();

    await user.selectOptions(await screen.findByLabelText("Strategy rating for Ngozi Member"), "4");

    expect(rateSkill).toHaveBeenCalledWith("s-1", "d-2", 4);
    await waitFor(() => expect(screen.queryByText(/only Chidi Chairman/)).not.toBeInTheDocument());
    expect(screen.getByLabelText("Strategy rating for Ngozi Member")).toHaveValue("4");
  });

  it("clears a rating when the blank option is chosen", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("ORG_ADMIN") } as ReturnType<typeof useAuth>);
    vi.mocked(rateSkill).mockResolvedValue(matrix);
    const user = userEvent.setup();
    renderPage();

    await user.selectOptions(await screen.findByLabelText("Audit rating for Ngozi Member"), "");

    expect(rateSkill).toHaveBeenCalledWith("s-2", "d-2", null);
  });

  it("adds and removes competencies", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("COMPANY_SECRETARY") } as ReturnType<typeof useAuth>);
    vi.mocked(addSkill).mockResolvedValue(matrix);
    vi.mocked(removeSkill).mockResolvedValue({ ...matrix, skills: matrix.skills.slice(0, 2) });
    const user = userEvent.setup();
    renderPage();

    await user.type(await screen.findByLabelText("New competency"), "  Insurance ");
    await user.selectOptions(screen.getByLabelText("Board requirement"), "HIGH");
    await user.click(screen.getByRole("button", { name: "Add competency" }));
    expect(addSkill).toHaveBeenCalledWith("board-1", { name: "Insurance", requiredLevel: "HIGH", futureFocus: false });

    const competencies = screen.getByRole("heading", { name: "Competencies" }).closest("section")!;
    const row = within(competencies).getByText("Cybersecurity").closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "Remove" }));
    expect(removeSkill).not.toHaveBeenCalled();
    await user.click(within(row).getByRole("button", { name: "Remove" }));
    expect(removeSkill).toHaveBeenCalledWith("s-3");
    expect(await within(competencies).findAllByRole("row")).toHaveLength(3);
  });

  it("is read-only for evaluators", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("EVALUATOR") } as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByRole("heading", { name: "Matrix" })).toBeInTheDocument();
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Competencies" })).not.toBeInTheDocument();
  });
});
