import client from "./client";

export function getServers() {
  return client.get("/federation/servers").then((res) => res.data);
}

export function addServer(baseUrl) {
  return client.post("/federation/servers", { baseUrl }).then((res) => res.data);
}

export function removeServer(id) {
  return client.delete(`/federation/servers/${id}`).then((res) => res.data);
}

export function syncNow() {
  return client.post("/federation/sync").then((res) => res.data);
}

export function getTimeline(page = 0, size = 20) {
  return client
    .get("/federation/timeline", { params: { page, size } })
    .then((res) => res.data);
}
