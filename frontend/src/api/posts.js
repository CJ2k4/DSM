import client from "./client";

export function getFeed(page = 0, size = 20) {
  return client.get("/posts/feed", { params: { page, size } }).then((res) => res.data);
}

export function getUserPosts(username, page = 0, size = 20) {
  return client
    .get(`/posts/user/${username}`, { params: { page, size } })
    .then((res) => res.data);
}

export function createPost({ content, imageUrl }) {
  return client
    .post("/posts", { content, imageUrl: imageUrl || null })
    .then((res) => res.data);
}

export function deletePost(id) {
  return client.delete(`/posts/${id}`).then((res) => res.data);
}

export function likePost(id) {
  return client.post(`/posts/${id}/like`).then((res) => res.data);
}

export function unlikePost(id) {
  return client.delete(`/posts/${id}/like`).then((res) => res.data);
}
