import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { getAccessToken } from "../api/tokenStore";
import { getPresence, getUnreadCount } from "../api/notifications";
import { useAuth } from "../hooks/useAuth";

// ws(s)://host/ws derived from the API base URL. A relative base (e.g.
// "/api/v1" behind the nginx proxy) resolves against the page's own origin.
const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";
const stripped = apiBase.replace(/\/api\/v1\/?$/, "");
const wsUrl = /^https?:/.test(apiBase)
  ? stripped.replace(/^http/, "ws") + "/ws"
  : (window.location.protocol === "https:" ? "wss" : "ws") +
    "://" +
    window.location.host +
    stripped +
    "/ws";

export const RealtimeContext = createContext(null);

export function useRealtime() {
  return useContext(RealtimeContext);
}

export function RealtimeProvider({ children }) {
  const { user } = useAuth();
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [unread, setUnread] = useState(0);
  const [liveNotifications, setLiveNotifications] = useState([]);
  const [presence, setPresence] = useState({ count: 0, usernames: [] });

  useEffect(() => {
    if (!user) {
      setUnread(0);
      setLiveNotifications([]);
      setPresence({ count: 0, usernames: [] });
      return undefined;
    }

    // Initial state via REST; live updates arrive over STOMP.
    getUnreadCount().then(setUnread).catch(() => {});
    getPresence().then(setPresence).catch(() => {});

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      // Re-read the token each (re)connect so a refreshed JWT is used.
      beforeConnect: () => {
        client.connectHeaders = { Authorization: `Bearer ${getAccessToken()}` };
      },
      onConnect: () => {
        setConnected(true);
        client.subscribe("/user/queue/notifications", (message) => {
          const notification = JSON.parse(message.body);
          setLiveNotifications((prev) => [notification, ...prev].slice(0, 50));
          setUnread((count) => count + 1);
        });
        client.subscribe("/topic/presence", (message) => {
          setPresence(JSON.parse(message.body));
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      clientRef.current = null;
      client.deactivate();
      setConnected(false);
    };
  }, [user]);

  // Live comment feed for one post; returns an unsubscribe function.
  const subscribeToComments = useCallback((postId, onComment) => {
    const client = clientRef.current;
    if (!client || !client.connected) return () => {};
    const subscription = client.subscribe(`/topic/posts/${postId}/comments`, (message) => {
      onComment(JSON.parse(message.body));
    });
    return () => subscription.unsubscribe();
  }, []);

  const clearUnread = useCallback(() => setUnread(0), []);

  const value = useMemo(
    () => ({
      connected,
      unread,
      clearUnread,
      liveNotifications,
      presence,
      subscribeToComments,
    }),
    [connected, unread, clearUnread, liveNotifications, presence, subscribeToComments]
  );

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}
