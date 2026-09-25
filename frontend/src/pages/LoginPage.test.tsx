import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { useAuth } from "../context/AuthContext";
import { LoginPage } from "./LoginPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={["/login"]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<div>Dashboard page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("LoginPage", () => {
  it("submits the entered email and password", async () => {
    const login = vi.fn().mockResolvedValue(undefined);
    mockedUseAuth.mockReturnValue({ login, loading: false } as unknown as ReturnType<typeof useAuth>);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email address"), "ada@example.com");
    await userEvent.type(screen.getByLabelText("Password"), "password123");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(login).toHaveBeenCalledWith({ email: "ada@example.com", password: "password123" });
    expect(await screen.findByText("Dashboard page")).toBeInTheDocument();
  });

  it("shows an error message when login fails", async () => {
    const login = vi.fn().mockRejectedValue(new Error("nope"));
    mockedUseAuth.mockReturnValue({ login, loading: false } as unknown as ReturnType<typeof useAuth>);

    renderLoginPage();
    await userEvent.type(screen.getByLabelText("Email address"), "ada@example.com");
    await userEvent.type(screen.getByLabelText("Password"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Something went wrong. Please try again.")).toBeInTheDocument();
  });
});
