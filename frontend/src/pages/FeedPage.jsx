import { useCallback, useEffect, useState } from "react";
import Layout from "../components/Layout";
import CreatePostForm from "../components/CreatePostForm";
import PostCard from "../components/PostCard";
import PostSkeleton from "../components/PostSkeleton";
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
    <Layout>
      <CreatePostForm onCreated={handleCreated} />

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <>
          <PostSkeleton />
          <PostSkeleton />
          <PostSkeleton />
        </>
      ) : posts.length === 0 ? (
        <div className="glass p-10 text-center">
          <p className="font-display text-lg font-semibold text-white">
            Your feed is quiet
          </p>
          <p className="mt-1.5 text-sm text-slate-400">
            Create your first post above, or follow people to see theirs.
          </p>
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
              className="btn-ghost w-full"
            >
              {loadingMore ? "Loading…" : "Load more"}
            </button>
          )}
        </>
      )}
    </Layout>
  );
}
