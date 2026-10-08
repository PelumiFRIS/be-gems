import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { changeCompanySecretaryAccess, createUser, listUsers, resetUserPassword } from "../api/users";
import { UsersPage } from "./UsersPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/users", () => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
  changeUserRole: vi.fn(),
  changeUserStatus: vi.fn(),
  resetUserPassword: vi.fn(),
  changeCompanySecretaryAccess: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedListUsers = vi.mocked(listUsers);
const mockedCreateUser = vi.mocked(createUser);
const mockedResetUserPassword = vi.mocked(resetUserPassword);
const mockedChangeCompanySecretaryAccess = vi.mocked(changeCompanySecretaryAccess);

const admin: UserSummary = {
  id: "admin-1",
  firstName: "Ada",
  lastName: "Admin",
  email: "ada@uitest.local",
  role: "ORG_ADMIN",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
  companySecretaryAccess: false,
};

const secretary: UserSummary = {
  ...admin,
  id: "cs-1",
  firstName: "Cara",
  lastName: "Secretary",
  email: "cara@uitest.local",
  role: "COMPANY_SECRETARY",
};

function renderPage() {
  return render(
    <MemoryRouter>
      <UsersPage />
    </MemoryRouter>,
  );
}

describe("UsersPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("lets an administrator give themselves Company Secretary access", async () => {
    const refreshUser = vi.fn().mockResolvedValue(undefined);
    mockedUseAuth.mockReturnValue({ user: admin, loading: false, refreshUser } as unknown as ReturnType<
      typeof useAuth
    >);
    mockedListUsers.mockResolvedValue([admin, secretary]);
    mockedChangeCompanySecretaryAccess.mockResolvedValue({ ...admin, companySecretaryAccess: true });
    const userEvents = userEvent.setup();
    renderPage();

    const checkboxes = await screen.findAllByRole("checkbox", { name: "Also Company Secretary" });
    expect(checkboxes).toHaveLength(1);
    await userEvents.click(checkboxes[0]);

    expect(mockedChangeCompanySecretaryAccess).toHaveBeenCalledWith("admin-1", true);
    expect(await screen.findByRole("checkbox", { name: "Also Company Secretary" })).toBeChecked();
    expect(refreshUser).toHaveBeenCalled();
  });

  it("creates a user and shows their temporary password once", async () => {
    mockedUseAuth.mockReturnValue({ user: admin, loading: false } as unknown as ReturnType<typeof useAuth>);
    mockedListUsers.mockResolvedValue([admin]);
    mockedCreateUser.mockResolvedValue({ user: secretary, temporaryPassword: "temp-pass-123" });
    const userEvents = userEvent.setup();
    renderPage();

    await userEvents.type(await screen.findByLabelText("First name"), "Cara");
    await userEvents.type(screen.getByLabelText("Last name"), "Secretary");
    await userEvents.type(screen.getByLabelText("Email"), "cara@uitest.local");
    await userEvents.click(screen.getByRole("button", { name: "Add user" }));

    expect(mockedCreateUser).toHaveBeenCalledWith({
      firstName: "Cara",
      lastName: "Secretary",
      email: "cara@uitest.local",
      role: "COMPANY_SECRETARY",
    });
    expect(await screen.findByText("temp-pass-123")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Login for Cara Secretary" })).toBeInTheDocument();
  });

  it("doesn't offer self-management actions on the admin's own row", async () => {
    mockedUseAuth.mockReturnValue({ user: admin, loading: false } as unknown as ReturnType<typeof useAuth>);
    mockedListUsers.mockResolvedValue([admin, secretary]);
    mockedResetUserPassword.mockResolvedValue({ email: secretary.email, temporaryPassword: "reset-456" });
    const userEvents = userEvent.setup();
    renderPage();

    const ownRow = (await screen.findByText("(you)")).closest("tr") as HTMLElement;
    expect(within(ownRow).queryByRole("button")).not.toBeInTheDocument();
    expect(within(ownRow).queryByRole("combobox")).not.toBeInTheDocument();

    const secretaryRow = screen.getByText("cara@uitest.local").closest("tr") as HTMLElement;
    await userEvents.click(within(secretaryRow).getByRole("button", { name: "Reset password" }));
    expect(mockedResetUserPassword).toHaveBeenCalledWith("cs-1");
    expect(await screen.findByText("reset-456")).toBeInTheDocument();
  });

  it("tells non-admins they can't manage users", async () => {
    mockedUseAuth.mockReturnValue({ user: secretary, loading: false } as unknown as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByText("Only an Organisation Administrator can manage users.")).toBeInTheDocument();
    expect(mockedListUsers).not.toHaveBeenCalled();
  });
});
