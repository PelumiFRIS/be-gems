import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AttachmentSummary, FindingSummary, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { getFinding } from "../api/findings";
import { listRecommendations } from "../api/recommendations";
import { listActions } from "../api/actions";
import { deleteAttachment, listAttachments, uploadAttachment } from "../api/attachments";
import { FindingDetailPage } from "./FindingDetailPage";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../api/findings", () => ({
  getFinding: vi.fn(),
}));

vi.mock("../api/recommendations", () => ({
  listRecommendations: vi.fn(),
  createRecommendation: vi.fn(),
}));

vi.mock("../api/actions", () => ({
  listActions: vi.fn(),
  createAction: vi.fn(),
}));

vi.mock("../api/attachments", () => ({
  listAttachments: vi.fn(),
  uploadAttachment: vi.fn(),
  deleteAttachment: vi.fn(),
  downloadAttachment: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);
const mockedGetFinding = vi.mocked(getFinding);
const mockedListRecommendations = vi.mocked(listRecommendations);
const mockedListActions = vi.mocked(listActions);
const mockedListAttachments = vi.mocked(listAttachments);
const mockedUploadAttachment = vi.mocked(uploadAttachment);
const mockedDeleteAttachment = vi.mocked(deleteAttachment);

const FINDING_ID = "finding-1";

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

const finding: FindingSummary = {
  id: FINDING_ID,
  evaluationId: "eval-1",
  dimensionId: null,
  dimensionName: "Board Composition",
  description: "Board packs circulated late",
  severity: "HIGH",
  evidence: null,
  regulatoryReference: null,
  rootCause: null,
  riskImplication: null,
  createdAt: "2026-01-01T00:00:00Z",
};

const existingAttachment: AttachmentSummary = {
  id: "att-1",
  findingId: FINDING_ID,
  fileName: "board-minutes.pdf",
  contentType: "application/pdf",
  fileSize: 20480,
  uploadedByName: "Deji Evaluator",
  createdAt: "2026-01-01T00:00:00Z",
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={[`/findings/${FINDING_ID}`]}>
      <Routes>
        <Route path="/findings/:findingId" element={<FindingDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("FindingDetailPage evidence attachments", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseAuth.mockReturnValue({ user, loading: false } as unknown as ReturnType<typeof useAuth>);
    mockedGetFinding.mockResolvedValue(finding);
    mockedListRecommendations.mockResolvedValue([]);
    mockedListActions.mockResolvedValue([]);
  });

  it("lists existing attachments and uploads a new one", async () => {
    mockedListAttachments.mockResolvedValue([existingAttachment]);
    const newAttachment: AttachmentSummary = {
      ...existingAttachment,
      id: "att-2",
      fileName: "risk-register.xlsx",
      fileSize: 4096,
    };
    mockedUploadAttachment.mockResolvedValue(newAttachment);

    renderPage();

    expect(await screen.findByText("board-minutes.pdf")).toBeInTheDocument();
    expect(screen.getByText(/20.0 KB/)).toBeInTheDocument();
    expect(screen.getByText(/Deji Evaluator/)).toBeInTheDocument();

    const file = new File(["sheet contents"], "risk-register.xlsx", { type: "application/vnd.ms-excel" });
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(fileInput, file);
    await userEvent.click(screen.getByRole("button", { name: "Upload evidence" }));

    expect(mockedUploadAttachment).toHaveBeenCalledWith(FINDING_ID, file);
    expect(await screen.findByText("risk-register.xlsx")).toBeInTheDocument();
  });

  it("deletes an attachment", async () => {
    mockedListAttachments.mockResolvedValue([existingAttachment]);
    mockedDeleteAttachment.mockResolvedValue(undefined);

    renderPage();

    expect(await screen.findByText("board-minutes.pdf")).toBeInTheDocument();
    await userEvent.click(screen.getByTitle("Delete"));

    expect(mockedDeleteAttachment).toHaveBeenCalledWith("att-1");
    expect(screen.queryByText("board-minutes.pdf")).not.toBeInTheDocument();
  });

  it("shows an empty state when there are no attachments", async () => {
    mockedListAttachments.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText("No evidence files uploaded yet.")).toBeInTheDocument();
  });
});
