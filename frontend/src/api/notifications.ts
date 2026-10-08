import { apiClient } from "./client";
import type { NotificationSummary } from "./types";

export async function listNotifications(): Promise<NotificationSummary[]> {
  const { data } = await apiClient.get<NotificationSummary[]>("/api/notifications");
  return data;
}

export async function getUnreadNotificationCount(): Promise<number> {
  const { data } = await apiClient.get<{ count: number }>("/api/notifications/unread-count");
  return data.count;
}

export async function markNotificationRead(notificationId: string): Promise<NotificationSummary> {
  const { data } = await apiClient.post<NotificationSummary>(`/api/notifications/${notificationId}/read`);
  return data;
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiClient.post("/api/notifications/read-all");
}
