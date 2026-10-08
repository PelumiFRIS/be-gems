import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listBoards } from "../api/boards";
import { listCommittees } from "../api/committees";
import { deleteDirector, listDirectors } from "../api/directors";
import type { DirectorSummary, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { BoardSetupPage } from "./BoardSetupPage";

vi.mock("../components/Sidebar", () => ({ Sidebar: () => null }));
vi.mock("../components/TopBar", () => ({ TopBar: () => null }));
vi.mock("../context/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../api/boards", () => ({ listBoards: vi.fn(), createBoard: vi.fn() }));
vi.mock("../api/committees", () => ({ listCommittees: vi.fn(), createCommittee: vi.fn() }));
vi.mock("../api/directors", () => ({
  listDirectors: vi.fn(),
  inviteDirector: vi.fn(),
  deleteDirector: vi.fn(),
}));

const mockedDeleteDirector = vi.mocked(deleteDirector);

const orgAdmin = {
  id: "u-1",
  firstName: "Modupeola",
  lastName: "Akanbi",
  email: "m@example.com",
  role: "ORG_ADMIN",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Acme",
} as UserSummary;

function director(id: string, name: string): DirectorSummary {
  return {
    id,
    boardId: "board-1",
    name,
    email: `${id}@example.com`,
    classification: "NON_EXECUTIVE_DIRECTOR",
    hasPortalAccess: false,
  } as DirectorSummary;
}

function renderPage() {
  return render(
    <MemoryRouter>
      <BoardSetupPage />
    </MemoryRouter>,
  );
}

describe("BoardSetupPage delete", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ user: orgAdmin } as ReturnType<typeof useAuth>);
    vi.mocked(listBoards).mockResolvedValue([{ id: "board-1", name: "Board of Directors" }] as Awaited<
      ReturnType<typeof listBoards>
    >);
    vi.mocked(listCommittees).mockResolvedValue([]);
    vi.mocked(listDirectors).mockResolvedValue([director("d-1", "Ada Lovelace"), director("d-2", "Ada Lovelace")]);
  });

  it("deletes a duplicated director from the list after confirming", async () => {
    mockedDeleteDirector.mockResolvedValue();
    const user = userEvent.setup();
    renderPage();

    const rows = await screen.findAllByRole("row");
    await user.click(within(rows[2]).getByRole("button", { name: "Delete" }));
    expect(mockedDeleteDirector).not.toHaveBeenCalled();
    await user.click(within(rows[2]).getByRole("button", { name: "Delete" }));

    expect(mockedDeleteDirector).toHaveBeenCalledWith("d-2");
    expect(await screen.findAllByRole("link", { name: "Ada Lovelace" })).toHaveLength(1);
  });

  it("shows the reason when a director can't be deleted", async () => {
    mockedDeleteDirector.mockRejectedValue(
      Object.assign(new Error("Conflict"), {
        isAxiosError: true,
        response: {
          status: 409,
          data: { message: "This director is part of an evaluation, so they can't be removed. Edit their details instead." },
        },
      }),
    );
    const user = userEvent.setup();
    renderPage();

    const rows = await screen.findAllByRole("row");
    await user.click(within(rows[1]).getByRole("button", { name: "Delete" }));
    await user.click(within(rows[1]).getByRole("button", { name: "Delete" }));

    expect(await screen.findByText(/part of an evaluation/)).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Ada Lovelace" })).toHaveLength(2);
  });
});
