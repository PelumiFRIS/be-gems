import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import type { UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { ProtectedRoute } from "./ProtectedRoute";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);

const user: UserSummary = {
  id: "user-1",
  firstName: "Ada",
  lastName: "Test",
  email: "ada@uitest.local",
  role: "ORG_ADMIN",
  status: "ACTIVE",
  organizationId: "org-1",
  organizationName: "Local UI Test Org",
};

function renderAt(path: string, ui: React.ReactNode) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={path} element={ui} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  it("shows a loading state while auth is resolving", () => {
    mockedUseAuth.mockReturnValue({ user: null, loading: true } as ReturnType<typeof useAuth>);
    renderAt("/board-setup", <ProtectedRoute>secret</ProtectedRoute>);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("redirects to /login when there is no user", () => {
    mockedUseAuth.mockReturnValue({ user: null, loading: false } as ReturnType<typeof useAuth>);
    renderAt("/board-setup", <ProtectedRoute>secret</ProtectedRoute>);
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("renders the route for a signed-in user", () => {
    mockedUseAuth.mockReturnValue({ user, loading: false } as ReturnType<typeof useAuth>);
    renderAt("/board-setup", <ProtectedRoute>secret content</ProtectedRoute>);
    expect(screen.getByText("secret content")).toBeInTheDocument();
  });
});
