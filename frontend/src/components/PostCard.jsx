import { useState } from "react";
import { Link } from "react-router-dom";
import { deletePost, likePost, unlikePost } from "../api/posts";
import { relativeTime } from "../utils/time";
import Avatar from "./Avatar";
import CommentSection from "./CommentSection";
import { CommentIcon, HeartIcon, TrashIcon } from "./icons";

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
    <article className="glass glass-hover p-4">
      <div className="mb-2.5 flex items-center justify-between">
        <Link
          to={`/profile/${post.author?.username}`}
          className="group flex items-center gap-2.5"
        >
          <Avatar user={post.author} className="h-9 w-9 text-sm" />
          <div className="leading-tight">
            <div className="text-sm font-semibold text-white group-hover:underline">
              {post.author?.displayName}
            </div>
            <div className="text-xs text-slate-500">
              @{post.author?.username} · {relativeTime(post.createdAt)}
            </div>
          </div>
        </Link>
        {isOwner && (
          <button
            onClick={handleDelete}
            disabled={deleting}
            title="Delete post"
            className="rounded-lg p-2 text-slate-600 transition hover:bg-rose-500/10 hover:text-rose-400 disabled:opacity-50"
          >
            <TrashIcon />
            <span className="sr-only">{deleting ? "Deleting…" : "Delete"}</span>
          </button>
        )}
      </div>

      <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed text-slate-200">
        {post.content}
      </p>

      {post.imageUrl && (
        <img
          src={post.imageUrl}
          alt=""
          className="mt-3 max-h-96 w-full rounded-xl object-cover ring-1 ring-white/10"
        />
      )}

      <div className="mt-3 flex items-center gap-2">
        <button
          onClick={toggleLike}
          disabled={busy}
          aria-label={liked ? "Unlike" : "Like"}
          className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition ${
            liked
              ? "bg-rose-500/10 text-rose-400"
              : "text-slate-500 hover:bg-rose-500/10 hover:text-rose-400"
          }`}
        >
          {/* Re-mounted on toggle so the pop animation replays. */}
          <span key={String(liked)} className={liked ? "inline-block animate-pop" : "inline-block"}>
            <HeartIcon filled={liked} />
          </span>
          <span className="tabular-nums">{likeCount}</span>
        </button>
        <button
          onClick={() => setShowComments((open) => !open)}
          aria-label="Comments"
          className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm transition ${
            showComments
              ? "bg-violet-500/10 text-violet-300"
              : "text-slate-500 hover:bg-violet-500/10 hover:text-violet-300"
          }`}
        >
          <CommentIcon />
          <span className="tabular-nums">{commentCount}</span>
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
