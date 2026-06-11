import { useCallback, useEffect, useState } from "react";
import Navbar from "../components/Navbar";
import CreatePostForm from "../components/CreatePostForm";
import PostCard from "../components/PostCard";
import { getFeed } from "../api/posts";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../hooks/useAuth";

export default function FeedPage() {
  const { user } = useAuth();
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  const loadPage = useCallback(async (pageNumber) => {
    const data = await getFeed(pageNumber);
    setPosts((prev) => (pageNumber === 0 ? data.content : [...prev, ...data.content]));
    setPage(data.number);
    setLast(data.last);
  }, []);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await loadPage(0);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load your feed"));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [loadPage]);

  async function handleLoadMore() {
    setLoadingMore(true);
    try {
      await loadPage(page + 1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not load more posts"));
    } finally {
      setLoadingMore(false);
    }
  }

  function handleCreated(post) {
    setPosts((prev) => [post, ...prev]);
  }

  function handleDeleted(postId) {
    setPosts((prev) => prev.filter((p) => p.id !== postId));
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-2xl space-y-4 px-4 py-6">
        <CreatePostForm onCreated={handleCreated} />

        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}

        {loading ? (
          <div className="py-10 text-center text-slate-400">Loading feed…</div>
        ) : posts.length === 0 ? (
          <div className="rounded-2xl bg-white p-8 text-center text-sm text-slate-500 shadow-sm">
            Your feed is empty. Create your first post above, or follow people to see theirs.
          </div>
        ) : (
          <>
            {posts.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                currentUserId={user?.id}
                onDeleted={handleDeleted}
              />
            ))}
            {!last && (
              <button
                onClick={handleLoadMore}
                disabled={loadingMore}
                className="w-full rounded-lg border border-slate-300 bg-white py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-60"
              >
                {loadingMore ? "Loading…" : "Load more"}
              </button>
            )}
          </>
        )}
      </main>
    </div>
  );
}
