import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getNotifications, markAllRead } from "../api/notifications";
import { useRealtime } from "../store/RealtimeContext";
import { relativeTime } from "../utils/time";
import Avatar from "./Avatar";
import { BellIcon } from "./icons";

const TEXT_BY_TYPE = {
  LIKE: "liked your post",
  COMMENT: "commented on your post",
  FOLLOW: "started following you",
};

export default function NotificationBell() {
  const { unread, clearUnread, liveNotifications } = useRealtime();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  // Opening the panel loads history and clears the badge.
  useEffect(() => {
    if (!open) return;
    setLoading(true);
    getNotifications(0)
      .then((data) => setItems(data.content))
      .catch(() => {})
      .finally(() => setLoading(false));
    markAllRead().catch(() => {});
    clearUnread();
  }, [open, clearUnread]);

  // Merge live pushes (newest first), deduped against loaded history.
  const loadedIds = new Set(items.map((n) => n.id));
  const merged = [...liveNotifications.filter((n) => !loadedIds.has(n.id)), ...items];

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        aria-label="Notifications"
        className={`relative rounded-lg p-2 transition ${
          open ? "bg-white/10 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
        }`}
      >
        <BellIcon />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 animate-pop items-center justify-center rounded-full bg-gradient-to-r from-rose-500 to-pink-500 px-1 text-[10px] font-bold leading-none text-white shadow-lg shadow-rose-500/40">
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </button>

      {open && (
        <>
          {/* click-away backdrop */}
          <div className="fixed inset-0 z-20" onClick={() => setOpen(false)} />
          <div className="glass absolute right-0 z-30 mt-2 w-80 animate-fade-up overflow-hidden p-0">
            <div className="border-b border-white/[0.06] px-4 py-3">
              <span className="font-display text-sm font-semibold text-white">
                Notifications
              </span>
            </div>
            <div className="max-h-96 overflow-y-auto">
              {loading ? (
                <div className="space-y-2 p-4">
                  <div className="skeleton h-8 w-full" />
                  <div className="skeleton h-8 w-3/4" />
                </div>
              ) : merged.length === 0 ? (
                <p className="px-4 py-8 text-center text-sm text-slate-500">
                  Nothing yet. When someone likes, comments, or follows, it shows up here.
                </p>
              ) : (
                merged.map((n) => (
                  <Link
                    key={n.id}
                    to={`/profile/${n.actor?.username}`}
                    onClick={() => setOpen(false)}
                    className="flex items-start gap-2.5 px-4 py-2.5 transition hover:bg-white/5"
                  >
                    <Avatar user={n.actor} className="h-8 w-8 text-xs" />
                    <div className="min-w-0 leading-snug">
                      <p className="text-sm text-slate-300">
                        <span className="font-semibold text-white">
                          {n.actor?.displayName}
                        </span>{" "}
                        {TEXT_BY_TYPE[n.type] || "did something"}
                      </p>
                      <span className="text-xs text-slate-500">
                        {relativeTime(n.createdAt)}
                      </span>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
