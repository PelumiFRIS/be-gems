import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { CommitteeSummary, DirectorDetail, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { addCommitteeMember, createCommittee, listCommittees } from "../api/committees";
import { getDirector } from "../api/directors";
import { DirectorProfilePage } from "./DirectorProfilePage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/directors", () => ({
  getDirector: vi.fn(),
  deleteDirector: vi.fn(),
  deleteDirectorCv: vi.fn(),
  downloadDirectorCv: vi.fn(),
  uploadDirectorCv: vi.fn(),
}));

vi.mock("../api/committees", () => ({
  listCommittees: vi.fn(),
  createCommittee: vi.fn(),
  addCommitteeMember: vi.fn(),
  removeCommitteeMember: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedGetDirector = vi.mocked(getDirector);
const mockedListCommittees = vi.mocked(listCommittees);
const mockedCreateCommittee = vi.mocked(createCommittee);
const mockedAddCommitteeMember = vi.mocked(addCommitteeMember);

const secretary: UserSummary = {
  id: "user-1",
  firstName: "Cara",
  lastName: "Secretary",
  email: "cara@uitest.local",
  role: "COMPANY_SECRETARY",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
};

const director: DirectorDetail = {
  id: "director-1",
  boardId: "board-1",
  name: "Adaeze Okafor",
  email: "adaeze@example.com",
  classification: "MD_CEO",
  appointmentDate: "2019-04-01",
  termExpirationDate: null,
  reElectionDate: "2023-05-10",
  dateOfBirth: "1972-08-15",
  phone: "+234 803 000 0000",
  address: "12 Marina, Lagos",
  profession: "Chartered Accountant",
  qualification: "FCA",
  experience: "25 years in banking",
  hasPortalAccess: false,
  cv: null,
  committees: [{ committeeId: "audit", committeeName: "Audit Committee", role: "CHAIR" }],
};

const audit: CommitteeSummary = {
  id: "audit",
  boardId: "board-1",
  name: "Audit Committee",
  meetingFrequency: null,
  mandate: null,
  members: [{ directorId: "director-1", role: "CHAIR" }],
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/board-setup/directors/director-1"]}>
      <Routes>
        <Route path="/board-setup/directors/:id" element={<DirectorProfilePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("DirectorProfilePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedGetDirector.mockResolvedValue(director);
    mockedListCommittees.mockResolvedValue([audit]);
  });

  it("shows the director's biodata and committee roles", async () => {
    mockedUseAuth.mockReturnValue({ user: secretary, loading: false } as unknown as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByRole("heading", { name: "Adaeze Okafor" })).toBeInTheDocument();
    expect(screen.getAllByText("MD/CEO").length).toBeGreaterThan(0);
    expect(screen.getByText("+234 803 000 0000")).toBeInTheDocument();
    expect(screen.getByText("15 Aug 1972")).toBeInTheDocument();
    expect(screen.getByText("10 May 2023")).toBeInTheDocument();
    expect(screen.getByLabelText("Role on Audit Committee")).toHaveValue("CHAIR");
    expect(screen.getByRole("link", { name: "Edit biodata" })).toHaveAttribute(
      "href",
      "/board-setup/directors/director-1/edit",
    );
  });

  it("creates a new committee and adds the director to it with the chosen role", async () => {
    mockedUseAuth.mockReturnValue({ user: secretary, loading: false } as unknown as ReturnType<typeof useAuth>);
    mockedCreateCommittee.mockResolvedValue({ ...audit, id: "risk", name: "Risk Committee", members: [] });
    mockedAddCommitteeMember.mockResolvedValue({ ...audit, id: "risk", name: "Risk Committee" });
    const userEvents = userEvent.setup();
    renderPage();

    await userEvents.selectOptions(await screen.findByLabelText("Committee"), "+ Create a new committee");
    await userEvents.type(screen.getByLabelText("New committee name"), "Risk Committee");
    await userEvents.selectOptions(screen.getByLabelText("Role"), "MEMBER");
    await userEvents.click(screen.getByRole("button", { name: "Add to committee" }));

    expect(mockedCreateCommittee).toHaveBeenCalledWith({ boardId: "board-1", name: "Risk Committee" });
    expect(mockedAddCommitteeMember).toHaveBeenCalledWith("risk", "director-1", "MEMBER");
  });

  it("hides editing controls from users who can't manage the board", async () => {
    mockedUseAuth.mockReturnValue({
      user: { ...secretary, role: "EVALUATOR" },
      loading: false,
    } as unknown as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByRole("heading", { name: "Adaeze Okafor" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Edit biodata" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Add to committee" })).not.toBeInTheDocument();
    expect(screen.getByText("Chair")).toBeInTheDocument();
    expect(mockedListCommittees).not.toHaveBeenCalled();
  });
});
