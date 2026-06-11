import { useState } from "react";
import { Link } from "react-router-dom";
import { deletePost, likePost, unlikePost } from "../api/posts";
import { relativeTime } from "../utils/time";
import Avatar from "./Avatar";
import CommentSection from "./CommentSection";

export default function PostCard({ post, currentUserId, onDeleted }) {
  const [liked, setLiked] = useState(post.likedByMe);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [commentCount, setCommentCount] = useState(post.commentCount);
  const [showComments, setShowComments] = useState(false);
  const [busy, setBusy] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const isOwner = currentUserId && post.author?.id === currentUserId;

  async function toggleLike() {
    if (busy) return;
    setBusy(true);
    // Optimistic update, reverted on failure.
    const nextLiked = !liked;
    setLiked(nextLiked);
    setLikeCount((c) => c + (nextLiked ? 1 : -1));
    try {
      const result = nextLiked ? await likePost(post.id) : await unlikePost(post.id);
      setLiked(result.liked);
      setLikeCount(result.likeCount);
    } catch {
      setLiked(!nextLiked);
      setLikeCount((c) => c + (nextLiked ? -1 : 1));
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete() {
    if (deleting) return;
    setDeleting(true);
    try {
      await deletePost(post.id);
      onDeleted?.(post.id);
    } catch {
      setDeleting(false);
    }
  }

  return (
    <article className="rounded-2xl bg-white p-4 shadow-sm">
      <div className="mb-2 flex items-center justify-between">
        <Link
          to={`/profile/${post.author?.username}`}
          className="group flex items-center gap-2"
        >
          <Avatar user={post.author} className="h-9 w-9 text-sm" />
          <div className="leading-tight">
            <div className="text-sm font-medium text-slate-900 group-hover:underline">
              {post.author?.displayName}
            </div>
            <div className="text-xs text-slate-400">
              @{post.author?.username} · {relativeTime(post.createdAt)}
            </div>
          </div>
        </Link>
        {isOwner && (
          <button
            onClick={handleDelete}
            disabled={deleting}
            className="text-xs font-medium text-slate-400 transition hover:text-red-600 disabled:opacity-50"
          >
            {deleting ? "Deleting…" : "Delete"}
          </button>
        )}
      </div>

      <p className="whitespace-pre-wrap break-words text-sm text-slate-800">
        {post.content}
      </p>

      {post.imageUrl && (
        <img
          src={post.imageUrl}
          alt=""
          className="mt-3 max-h-96 w-full rounded-xl object-cover"
        />
      )}

      <div className="mt-3 flex items-center gap-4 text-sm">
        <button
          onClick={toggleLike}
          disabled={busy}
          className={`flex items-center gap-1 transition ${
            liked ? "text-red-600" : "text-slate-500 hover:text-red-600"
          }`}
        >
          <span>{liked ? "♥" : "♡"}</span>
          <span>{likeCount}</span>
        </button>
        <button
          onClick={() => setShowComments((open) => !open)}
          className={`flex items-center gap-1 transition ${
            showComments ? "text-blue-600" : "text-slate-500 hover:text-blue-600"
          }`}
        >
          <span>💬</span>
          <span>{commentCount}</span>
        </button>
      </div>

      {showComments && (
        <CommentSection
          postId={post.id}
          postOwnerId={post.author?.id}
          onCountChange={(delta) => setCommentCount((c) => c + delta)}
        />
      )}
    </article>
  );
}
