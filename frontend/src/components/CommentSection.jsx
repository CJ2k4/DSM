import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { addComment, deleteComment, getComments } from "../api/comments";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../hooks/useAuth";
import { relativeTime } from "../utils/time";
import Avatar from "./Avatar";
import { TrashIcon } from "./icons";

export default function CommentSection({ postId, postOwnerId, onCountChange }) {
  const { user } = useAuth();
  const [comments, setComments] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const [draft, setDraft] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function loadPage(pageNumber) {
    const data = await getComments(postId, pageNumber);
    setComments((prev) =>
      pageNumber === 0 ? data.content : [...prev, ...data.content]
    );
    setPage(data.number);
    setLast(data.last);
  }

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await loadPage(0);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load comments"));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postId]);

  async function handleLoadMore() {
    setLoadingMore(true);
    try {
      await loadPage(page + 1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not load more comments"));
    } finally {
      setLoadingMore(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const created = await addComment(postId, content);
      setComments((prev) => [...prev, created]);
      setDraft("");
      onCountChange?.(1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not add comment"));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(commentId) {
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      onCountChange?.(-1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not delete comment"));
    }
  }

  // The comment author or the post owner may delete a comment.
  function canDelete(comment) {
    return user && (comment.author?.id === user.id || postOwnerId === user.id);
  }

  return (
    <div className="mt-3 animate-fade-up border-t border-white/[0.06] pt-3">
      {error && (
        <div className="error-banner mb-2 px-3 py-2 text-xs">{error}</div>
      )}

      {loading ? (
        <div className="space-y-2 py-1">
          <div className="skeleton h-9 w-full rounded-xl" />
          <div className="skeleton h-9 w-3/4 rounded-xl" />
        </div>
      ) : (
        <div className="space-y-3">
          {comments.length === 0 && (
            <p className="text-xs text-slate-500">No comments yet. Be the first!</p>
          )}
          {comments.map((comment) => (
            <div key={comment.id} className="flex items-start gap-2">
              <Avatar user={comment.author} className="h-7 w-7 text-xs" />
              <div className="min-w-0 flex-1 rounded-xl border border-white/[0.05] bg-white/[0.03] px-3 py-2">
                <div className="flex items-center justify-between gap-2">
                  <Link
                    to={`/profile/${comment.author?.username}`}
                    className="truncate text-xs font-semibold text-slate-200 hover:underline"
                  >
                    {comment.author?.displayName}
                    <span className="ml-1 font-normal text-slate-500">
                      @{comment.author?.username} · {relativeTime(comment.createdAt)}
                    </span>
                  </Link>
                  {canDelete(comment) && (
                    <button
                      onClick={() => handleDelete(comment.id)}
                      title="Delete comment"
                      className="rounded p-1 text-slate-600 transition hover:bg-rose-500/10 hover:text-rose-400"
                    >
                      <TrashIcon className="h-3.5 w-3.5" />
                      <span className="sr-only">Delete</span>
                    </button>
                  )}
                </div>
                <p className="whitespace-pre-wrap break-words text-sm text-slate-300">
                  {comment.content}
                </p>
              </div>
            </div>
          ))}
          {!last && (
            <button
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="btn-ghost w-full py-1.5 text-xs"
            >
              {loadingMore ? "Loading…" : "Load more comments"}
            </button>
          )}
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-3 flex items-center gap-2">
        <input
          type="text"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Write a comment…"
          maxLength={500}
          className="field py-2"
        />
        <button
          type="submit"
          disabled={submitting || !draft.trim()}
          className="btn-primary px-3.5 py-2"
        >
          {submitting ? "…" : "Post"}
        </button>
      </form>
    </div>
  );
}
