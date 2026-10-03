import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { DirectorSummary, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { listBoards } from "../api/boards";
import { createDirector } from "../api/directors";
import { DirectorFormPage } from "./DirectorFormPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/boards", () => ({
  listBoards: vi.fn(),
}));

vi.mock("../api/directors", () => ({
  createDirector: vi.fn(),
  getDirector: vi.fn(),
  updateDirector: vi.fn(),
  uploadDirectorCv: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedListBoards = vi.mocked(listBoards);
const mockedCreateDirector = vi.mocked(createDirector);

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

const created: DirectorSummary = {
  id: "director-1",
  boardId: "board-1",
  name: "Tunde Bakare",
  email: "tunde@example.com",
  classification: "MD_CEO",
  appointmentDate: null,
  termExpirationDate: null,
  hasPortalAccess: false,
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/board-setup/directors/new"]}>
      <Routes>
        <Route path="/board-setup/directors/new" element={<DirectorFormPage />} />
        <Route path="/board-setup/directors/:id" element={<p>Profile page</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("DirectorFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({ user, loading: false, logout: vi.fn() } as unknown as ReturnType<typeof useAuth>);
    mockedListBoards.mockResolvedValue([{ id: "board-1", name: "Board", effectiveDate: null, notes: null }]);
  });

  it("labels the managing director classification as MD/CEO", async () => {
    renderPage();
    expect(await screen.findByRole("option", { name: "MD/CEO" })).toBeInTheDocument();
    expect(screen.queryByRole("option", { name: /CEO\/MD|CEO MD/ })).not.toBeInTheDocument();
  });

  it("requires an email", async () => {
    renderPage();
    expect(await screen.findByLabelText(/Email \*/)).toBeRequired();
  });

  it("sends only one create request when the form is submitted repeatedly while saving", async () => {
    let resolveCreate: (value: DirectorSummary) => void = () => {};
    mockedCreateDirector.mockReturnValue(new Promise((resolve) => (resolveCreate = resolve)));
    const userEvents = userEvent.setup();
    renderPage();

    await userEvents.type(await screen.findByLabelText(/Full name \*/), "Tunde Bakare");
    await userEvents.type(screen.getByLabelText(/Email \*/), "tunde@example.com");
    await userEvents.selectOptions(screen.getByLabelText(/Classification \*/), "MD_CEO");
    await userEvents.type(screen.getByLabelText("Profession"), "Banker");

    const submit = screen.getByRole("button", { name: "Add director" });
    await userEvents.click(submit);
    await userEvents.click(submit);
    await userEvents.click(submit);

    expect(screen.getByRole("button", { name: "Saving..." })).toBeDisabled();
    expect(mockedCreateDirector).toHaveBeenCalledTimes(1);
    expect(mockedCreateDirector).toHaveBeenCalledWith(
      expect.objectContaining({
        boardId: "board-1",
        name: "Tunde Bakare",
        email: "tunde@example.com",
        classification: "MD_CEO",
        profession: "Banker",
        phone: undefined,
      }),
    );

    resolveCreate(created);
    expect(await screen.findByText("Profile page")).toBeInTheDocument();
  });
});
