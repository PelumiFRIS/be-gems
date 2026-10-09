import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getBenchmarks, resetBenchmark, setBenchmark } from "../api/benchmarks";
import type { BenchmarkSettings, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { BenchmarksPage } from "./BenchmarksPage";

vi.mock("../components/Sidebar", () => ({ Sidebar: () => null }));
vi.mock("../components/TopBar", () => ({ TopBar: () => null }));
vi.mock("../context/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../api/benchmarks", () => ({
  getBenchmarks: vi.fn(),
  setBenchmark: vi.fn(),
  resetBenchmark: vi.fn(),
}));

const DEFAULT_SOURCE = "BE-GEMS default: maturity Level 3 (Defined)";

const settings: BenchmarkSettings = {
  dimensions: [
    { dimensionId: "dim-1", name: "Board Composition", target: 3, source: DEFAULT_SOURCE, isDefault: true },
    { dimensionId: "dim-2", name: "Strategy", target: 4.2, source: "CBN Code 2023, s.5", isDefault: false },
  ],
  bgei: {
    dimensionId: null,
    name: "BGEI",
    target: 70,
    source: "BE-GEMS default: the lower bound of the Effective band",
    isDefault: true,
  },
  defaultDimensionTarget: 3,
  defaultBgeiTarget: 70,
};

function userWithRole(role: UserSummary["role"]): UserSummary {
  return { id: "u-1", firstName: "Cara", lastName: "Secretary", role } as UserSummary;
}

function renderPage() {
  return render(
    <MemoryRouter>
      <BenchmarksPage />
    </MemoryRouter>,
  );
}

describe("BenchmarksPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getBenchmarks).mockResolvedValue(settings);
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("COMPANY_SECRETARY") } as ReturnType<typeof useAuth>);
  });

  it("shows each area's benchmark and its basis", async () => {
    renderPage();

    const strategy = (await screen.findByText("Strategy")).closest("tr")!;
    expect(within(strategy).getByText("4.20")).toBeInTheDocument();
    expect(within(strategy).getByText("CBN Code 2023, s.5")).toBeInTheDocument();
    const bgei = screen.getByText("Board Governance Effectiveness Index (BGEI)").closest("tr")!;
    expect(within(bgei).getByText("70.00%")).toBeInTheDocument();
    expect(within(bgei).queryByRole("button", { name: "Use default" })).not.toBeInTheDocument();
  });

  it("sets a dimension benchmark with its basis", async () => {
    vi.mocked(setBenchmark).mockResolvedValue({
      ...settings,
      dimensions: [
        { dimensionId: "dim-1", name: "Board Composition", target: 3.5, source: "SEC Code 2011", isDefault: false },
        settings.dimensions[1],
      ],
    });
    const user = userEvent.setup();
    renderPage();

    const row = (await screen.findByText("Board Composition")).closest("tr")!;
    await user.click(within(row).getByRole("button", { name: "Edit" }));
    const target = screen.getByLabelText("Benchmark (0–5)");
    await user.clear(target);
    await user.type(target, "3.5");
    await user.type(screen.getByLabelText("Basis"), " SEC Code 2011 ");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(setBenchmark).toHaveBeenCalledWith("dim-1", { target: 3.5, source: "SEC Code 2011" });
    expect(await screen.findByText("SEC Code 2011")).toBeInTheDocument();
  });

  it("edits the BGEI as a percentage and resets a custom benchmark to the default", async () => {
    vi.mocked(setBenchmark).mockResolvedValue(settings);
    vi.mocked(resetBenchmark).mockResolvedValue(settings);
    const user = userEvent.setup();
    renderPage();

    const bgei = (await screen.findByText("Board Governance Effectiveness Index (BGEI)")).closest("tr")!;
    await user.click(within(bgei).getByRole("button", { name: "Edit" }));
    const target = screen.getByLabelText("Benchmark (%)");
    expect(target).toHaveAttribute("max", "100");
    await user.clear(target);
    await user.type(target, "75");
    await user.click(screen.getByRole("button", { name: "Save" }));
    expect(setBenchmark).toHaveBeenCalledWith(null, { target: 75, source: null });

    const strategy = (await screen.findByText("Strategy")).closest("tr")!;
    await user.click(within(strategy).getByRole("button", { name: "Use default" }));
    expect(resetBenchmark).toHaveBeenCalledWith("dim-2");
  });

  it("is read-only for evaluators", async () => {
    vi.mocked(useAuth).mockReturnValue({ user: userWithRole("EVALUATOR") } as ReturnType<typeof useAuth>);
    renderPage();

    expect(await screen.findByText("Strategy")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Edit" })).not.toBeInTheDocument();
  });
});
