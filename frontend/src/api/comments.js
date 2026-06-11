import client from "./client";

export function getComments(postId, page = 0, size = 20) {
  return client
    .get(`/posts/${postId}/comments`, { params: { page, size } })
    .then((res) => res.data);
}

export function addComment(postId, content) {
  return client.post(`/posts/${postId}/comments`, { content }).then((res) => res.data);
}

export function deleteComment(commentId) {
  return client.delete(`/comments/${commentId}`).then((res) => res.data);
}
