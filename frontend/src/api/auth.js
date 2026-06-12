import client from "./client";

export function register({ username, email, password, displayName }) {
  return client
    .post("/auth/register", { username, email, password, displayName })
    .then((res) => res.data);
}

export function login({ identifier, password }) {
  return client.post("/auth/login", { identifier, password }).then((res) => res.data);
}

export function refresh(refreshToken) {
  return client.post("/auth/refresh", { refreshToken }).then((res) => res.data);
}

export function logout(refreshToken) {
  return client.post("/auth/logout", { refreshToken }).then((res) => res.data);
}

export function me() {
  return client.get("/auth/me").then((res) => res.data);
}
