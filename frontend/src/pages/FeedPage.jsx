import { useCallback, useEffect, useState } from "react";
import Layout from "../components/Layout";
import CreatePostForm from "../components/CreatePostForm";
import PostCard from "../components/PostCard";
import RemotePostCard from "../components/RemotePostCard";
import PostSkeleton from "../components/PostSkeleton";
import { getFeed } from "../api/posts";
import { getTimeline } from "../api/federation";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../hooks/useAuth";

const TABS = ["Following", "Global"];

// Paginated list state for one feed source.
function useFeedSource(fetcher) {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  const loadPage = useCallback(async (pageNumber) => {
    const data = await fetcher(pageNumber);
    setItems((prev) => (pageNumber === 0 ? data.content : [...prev, ...data.content]));
    setPage(data.number);
    setLast(data.last);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        await loadPage(0);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load posts"));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [loadPage]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      await loadPage(page + 1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not load more posts"));
    } finally {
      setLoadingMore(false);
    }
  }

  return { items, setItems, last, loading, loadingMore, error, loadMore };
}

export default function FeedPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState("Following");

  const following = useFeedSource((p) => getFeed(p));
  const global = useFeedSource((p) => getTimeline(p));

  function handleCreated(post) {
    following.setItems((prev) => [post, ...prev]);
  }

  function handleDeleted(postId) {
    following.setItems((prev) => prev.filter((p) => p.id !== postId));
  }

  const active = tab === "Following" ? following : global;

  return (
    <Layout>
      <CreatePostForm onCreated={handleCreated} />

      <nav className="glass flex p-1">
        {TABS.map((name) => (
          <button
            key={name}
            onClick={() => setTab(name)}
            className={`flex-1 rounded-xl py-2 text-sm font-medium transition ${
              tab === name
                ? "bg-white/10 text-white shadow-inner"
                : "text-slate-500 hover:text-slate-300"
            }`}
          >
            {name}
          </button>
        ))}
      </nav>

      {active.error && <div className="error-banner">{active.error}</div>}

      {active.loading ? (
        <>
          <PostSkeleton />
          <PostSkeleton />
          <PostSkeleton />
        </>
      ) : active.items.length === 0 ? (
        <div className="glass p-10 text-center">
          <p className="font-display text-lg font-semibold text-white">
            {tab === "Following" ? "Your feed is quiet" : "Nothing from the network yet"}
          </p>
          <p className="mt-1.5 text-sm text-slate-400">
            {tab === "Following"
              ? "Create your first post above, or follow people to see theirs."
              : "Add peer servers on the Network page, then sync to see their posts here."}
          </p>
        </div>
      ) : (
        <>
          {tab === "Following"
            ? active.items.map((post) => (
                <PostCard
                  key={post.id}
                  post={post}
                  currentUserId={user?.id}
                  onDeleted={handleDeleted}
                />
              ))
            : active.items.map((post) => <RemotePostCard key={post.id} post={post} />)}
          {!active.last && (
            <button
              onClick={active.loadMore}
              disabled={active.loadingMore}
              className="btn-ghost w-full"
            >
              {active.loadingMore ? "Loading…" : "Load more"}
            </button>
          )}
        </>
      )}
    </Layout>
  );
}
