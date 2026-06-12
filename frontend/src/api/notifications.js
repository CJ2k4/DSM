import client from "./client";

export function getNotifications(page = 0, size = 20) {
  return client.get("/notifications", { params: { page, size } }).then((res) => res.data);
}

export function getUnreadCount() {
  return client.get("/notifications/unread-count").then((res) => res.data.count);
}

export function markAllRead() {
  return client.post("/notifications/read-all").then((res) => res.data);
}

export function getPresence() {
  return client.get("/presence").then((res) => res.data);
}
