import client from "./client";

export function getProfile(username) {
  return client.get(`/users/${username}`).then((res) => res.data);
}

export function updateProfile({ displayName, bio, avatarUrl, bannerUrl }) {
  return client
    .patch("/users/me", { displayName, bio, avatarUrl, bannerUrl })
    .then((res) => res.data);
}

export function searchUsers(query, page = 0, size = 20) {
  return client
    .get("/users/search", { params: { q: query, page, size } })
    .then((res) => res.data);
}

export function followUser(username) {
  return client.post(`/users/${username}/follow`).then((res) => res.data);
}

export function unfollowUser(username) {
  return client.delete(`/users/${username}/follow`).then((res) => res.data);
}

export function getFollowers(username, page = 0, size = 20) {
  return client
    .get(`/users/${username}/followers`, { params: { page, size } })
    .then((res) => res.data);
}

export function getFollowing(username, page = 0, size = 20) {
  return client
    .get(`/users/${username}/following`, { params: { page, size } })
    .then((res) => res.data);
}
