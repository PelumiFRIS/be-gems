import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "../api/notifications";
import type { NotificationSummary } from "../api/types";
import { NotificationBell } from "./NotificationBell";

vi.mock("../api/notifications", () => ({
  getUnreadNotificationCount: vi.fn(),
  listNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}));

const mockedUnreadCount = vi.mocked(getUnreadNotificationCount);
const mockedList = vi.mocked(listNotifications);
const mockedMarkRead = vi.mocked(markNotificationRead);
const mockedMarkAllRead = vi.mocked(markAllNotificationsRead);

const invitation: NotificationSummary = {
  id: "n-1",
  type: "EVALUATION_INVITATION",
  title: "You're invited to complete the 2026 Board Evaluation",
  body: "Acme Plc has opened the 2026 Board Evaluation. Please complete your questionnaire.",
  link: "/my-evaluations/eval-1",
  read: false,
  createdAt: new Date().toISOString(),
};

const confirmation: NotificationSummary = {
  id: "n-2",
  type: "SUBMISSION_CONFIRMED",
  title: "Thank you — your 2025 Board Evaluation response was received",
  body: null,
  link: "/my-evaluations",
  read: true,
  createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
};

function renderBell() {
  return render(
    <MemoryRouter initialEntries={["/dashboard"]}>
      <Routes>
        <Route path="/dashboard" element={<NotificationBell />} />
        <Route path="/my-evaluations/:id" element={<p>Questionnaire page</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("NotificationBell", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUnreadCount.mockResolvedValue(1);
    mockedList.mockResolvedValue([invitation, confirmation]);
    mockedMarkRead.mockResolvedValue({ ...invitation, read: true });
    mockedMarkAllRead.mockResolvedValue();
  });

  it("shows the unread count on the bell", async () => {
    renderBell();
    expect(await screen.findByRole("button", { name: "Notifications, 1 unread" })).toHaveTextContent("1");
  });

  it("hides the badge when there is nothing unread", async () => {
    mockedUnreadCount.mockResolvedValue(0);
    renderBell();
    await screen.findByRole("button", { name: "Notifications" });
    expect(mockedUnreadCount).toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Notifications" })).not.toHaveTextContent(/\d/);
  });

  it("lists recent notifications and opens the linked page, marking it read", async () => {
    const user = userEvent.setup();
    renderBell();
    await user.click(await screen.findByRole("button", { name: "Notifications, 1 unread" }));

    expect(await screen.findByText(invitation.title)).toBeInTheDocument();
    expect(screen.getByText(confirmation.title)).toBeInTheDocument();
    expect(screen.getByText("3 days ago")).toBeInTheDocument();

    await user.click(screen.getByText(invitation.title));

    expect(mockedMarkRead).toHaveBeenCalledWith("n-1");
    expect(await screen.findByText("Questionnaire page")).toBeInTheDocument();
  });

  it("marks everything read", async () => {
    const user = userEvent.setup();
    renderBell();
    await user.click(await screen.findByRole("button", { name: "Notifications, 1 unread" }));
    await user.click(await screen.findByRole("button", { name: "Mark all as read" }));

    expect(mockedMarkAllRead).toHaveBeenCalled();
    expect(await screen.findByRole("button", { name: "Notifications" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Mark all as read" })).not.toBeInTheDocument();
  });

  it("shows an empty state", async () => {
    mockedUnreadCount.mockResolvedValue(0);
    mockedList.mockResolvedValue([]);
    const user = userEvent.setup();
    renderBell();
    await user.click(await screen.findByRole("button", { name: "Notifications" }));
    expect(await screen.findByText("You're all caught up.")).toBeInTheDocument();
  });
});
