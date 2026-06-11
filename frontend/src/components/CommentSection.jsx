import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { addComment, deleteComment, getComments } from "../api/comments";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../hooks/useAuth";
import { relativeTime } from "../utils/time";
import Avatar from "./Avatar";

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
    <div className="mt-3 border-t border-slate-100 pt-3">
      {error && (
        <div className="mb-2 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-2 text-center text-xs text-slate-400">
          Loading comments…
        </div>
      ) : (
        <div className="space-y-3">
          {comments.length === 0 && (
            <p className="text-xs text-slate-400">No comments yet. Be the first!</p>
          )}
          {comments.map((comment) => (
            <div key={comment.id} className="flex items-start gap-2">
              <Avatar user={comment.author} className="h-7 w-7 text-xs" />
              <div className="min-w-0 flex-1 rounded-xl bg-slate-50 px-3 py-2">
                <div className="flex items-center justify-between gap-2">
                  <Link
                    to={`/profile/${comment.author?.username}`}
                    className="truncate text-xs font-medium text-slate-900 hover:underline"
                  >
                    {comment.author?.displayName}
                    <span className="ml-1 font-normal text-slate-400">
                      @{comment.author?.username} · {relativeTime(comment.createdAt)}
                    </span>
                  </Link>
                  {canDelete(comment) && (
                    <button
                      onClick={() => handleDelete(comment.id)}
                      className="text-xs text-slate-400 transition hover:text-red-600"
                    >
                      Delete
                    </button>
                  )}
                </div>
                <p className="whitespace-pre-wrap break-words text-sm text-slate-800">
                  {comment.content}
                </p>
              </div>
            </div>
          ))}
          {!last && (
            <button
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="w-full rounded-lg border border-slate-200 py-1.5 text-xs font-medium text-slate-500 transition hover:bg-slate-50 disabled:opacity-60"
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
          className="w-full rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <button
          type="submit"
          disabled={submitting || !draft.trim()}
          className="rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
        >
          {submitting ? "…" : "Post"}
        </button>
      </form>
    </div>
  );
}
