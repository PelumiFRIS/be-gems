import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "../api/notifications";
import type { NotificationSummary } from "../api/types";
import { BellIcon } from "./icons";

const POLL_INTERVAL_MS = 60_000;

function timeAgo(iso: string): string {
  const seconds = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "Just now";
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.round(hours / 24);
  if (days < 7) return days === 1 ? "Yesterday" : `${days} days ago`;
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

export function NotificationBell() {
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<NotificationSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refreshCount = useCallback(() => {
    getUnreadNotificationCount()
      .then(setUnread)
      .catch(() => {
        // A failed badge refresh isn't worth interrupting the user; the next poll retries.
      });
  }, []);

  useEffect(() => {
    refreshCount();
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") refreshCount();
    }, POLL_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return;
    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) setOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  function toggle() {
    const next = !open;
    setOpen(next);
    if (next) {
      setError(null);
      listNotifications()
        .then((list) => {
          setItems(list);
          setUnread(list.filter((n) => !n.read).length);
        })
        .catch((err) => setError(extractErrorMessage(err)));
    }
  }

  function handleSelect(notification: NotificationSummary) {
    setOpen(false);
    if (!notification.read) {
      setItems((prev) => prev?.map((n) => (n.id === notification.id ? { ...n, read: true } : n)) ?? prev);
      setUnread((count) => Math.max(0, count - 1));
      markNotificationRead(notification.id).catch(() => refreshCount());
    }
    if (notification.link) navigate(notification.link);
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsRead();
      setItems((prev) => prev?.map((n) => ({ ...n, read: true })) ?? prev);
      setUnread(0);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="notification-bell" ref={containerRef}>
      <button
        type="button"
        className="secondary small notification-bell-button"
        onClick={toggle}
        aria-haspopup="true"
        aria-expanded={open}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}
      >
        <BellIcon width={17} height={17} />
        {unread > 0 && <span className="notification-badge">{unread > 99 ? "99+" : unread}</span>}
      </button>

      {open && (
        <div className="notification-panel" role="dialog" aria-label="Notifications">
          <div className="notification-panel-header">
            <strong>Notifications</strong>
            {unread > 0 && (
              <button type="button" className="link-button" onClick={handleMarkAllRead}>
                Mark all as read
              </button>
            )}
          </div>
          {error && <p className="form-error">{error}</p>}
          {!error && items === null && <p className="notification-empty">Loading...</p>}
          {items !== null && items.length === 0 && <p className="notification-empty">You're all caught up.</p>}
          {items !== null && items.length > 0 && (
            <ul className="notification-list">
              {items.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={`notification-item${n.read ? "" : " unread"}`}
                    onClick={() => handleSelect(n)}
                  >
                    <span className="notification-title">{n.title}</span>
                    {n.body && <span className="notification-body">{n.body}</span>}
                    <span className="notification-time">{timeAgo(n.createdAt)}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
