import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Layout from "../components/Layout";
import Avatar from "../components/Avatar";
import PostCard from "../components/PostCard";
import PostSkeleton from "../components/PostSkeleton";
import UserCard from "../components/UserCard";
import EditProfileForm from "../components/EditProfileForm";
import { extractErrorMessage } from "../api/client";
import { getUserPosts } from "../api/posts";
import { followUser, getFollowers, getFollowing, getProfile, unfollowUser } from "../api/users";
import { useAuth } from "../hooks/useAuth";

const TABS = ["Posts", "Followers", "Following"];

function joinedDate(instant) {
  if (!instant) return "";
  return new Date(instant).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });
}

// Paginated tab list — fetches a Page<> of posts or users for the active tab.
function useTabPage(fetcher, deps) {
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
  }, deps);

  useEffect(() => {
    let active = true;
    setItems([]);
    setLoading(true);
    setError(null);
    (async () => {
      try {
        await loadPage(0);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load this list"));
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
      setError(extractErrorMessage(err, "Could not load more"));
    } finally {
      setLoadingMore(false);
    }
  }

  return { items, setItems, last, loading, loadingMore, error, loadMore };
}

export default function ProfilePage() {
  const { username } = useParams();
  const { user: currentUser, updateUser } = useAuth();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [followBusy, setFollowBusy] = useState(false);
  const [editing, setEditing] = useState(false);
  const [tab, setTab] = useState("Posts");

  useEffect(() => {
    let active = true;
    setProfile(null);
    setLoading(true);
    setError(null);
    setEditing(false);
    setTab("Posts");
    (async () => {
      try {
        const data = await getProfile(username);
        if (active) setProfile(data);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load this profile"));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [username]);

  const postsTab = useTabPage((p) => getUserPosts(username, p), [username]);
  const followersTab = useTabPage((p) => getFollowers(username, p), [username]);
  const followingTab = useTabPage((p) => getFollowing(username, p), [username]);

  async function toggleFollow() {
    if (followBusy || !profile) return;
    setFollowBusy(true);
    try {
      const result = profile.following
        ? await unfollowUser(username)
        : await followUser(username);
      setProfile((prev) => ({
        ...prev,
        following: result.following,
        followerCount: result.followerCount,
        followingCount: result.followingCount,
      }));
    } catch (err) {
      setError(extractErrorMessage(err, "Could not update follow status"));
    } finally {
      setFollowBusy(false);
    }
  }

  function handleSaved(updated) {
    setProfile((prev) => ({ ...prev, ...updated }));
    updateUser(updated);
    setEditing(false);
  }

  function handlePostDeleted(postId) {
    postsTab.setItems((prev) => prev.filter((p) => p.id !== postId));
  }

  const activeTab =
    tab === "Posts" ? postsTab : tab === "Followers" ? followersTab : followingTab;

  return (
    <Layout>
      {loading ? (
        <div className="glass p-4">
          <div className="skeleton h-24 w-full rounded-xl" />
          <div className="mt-4 flex items-center gap-3">
            <div className="skeleton h-20 w-20 rounded-full" />
            <div className="space-y-2">
              <div className="skeleton h-4 w-40" />
              <div className="skeleton h-3 w-24" />
            </div>
          </div>
        </div>
      ) : error && !profile ? (
        <div className="error-banner">{error}</div>
      ) : (
        <>
          <section className="glass overflow-hidden">
            {profile.bannerUrl ? (
              <img src={profile.bannerUrl} alt="" className="h-32 w-full object-cover" />
            ) : (
              <div className="relative h-24 overflow-hidden bg-gradient-to-r from-indigo-600/30 via-violet-600/25 to-cyan-500/25">
                <div className="absolute inset-0 bg-[radial-gradient(rgba(255,255,255,0.08)_1px,transparent_1px)] [background-size:20px_20px]" />
              </div>
            )}
            <div className="p-5">
              <div className="-mt-14 mb-3 flex items-end justify-between">
                <div className="rounded-full ring-4 ring-[#101018]">
                  <Avatar user={profile} className="h-20 w-20 text-2xl" />
                </div>
                {profile.ownProfile ? (
                  <button
                    onClick={() => setEditing((open) => !open)}
                    className="btn-ghost px-3 py-1.5"
                  >
                    {editing ? "Cancel" : "Edit profile"}
                  </button>
                ) : (
                  <button
                    onClick={toggleFollow}
                    disabled={followBusy}
                    className={
                      profile.following ? "btn-ghost px-4 py-1.5" : "btn-primary px-5 py-1.5"
                    }
                  >
                    {profile.following ? "Unfollow" : "Follow"}
                  </button>
                )}
              </div>

              <h1 className="font-display text-xl font-bold tracking-tight text-white">
                {profile.displayName}
              </h1>
              <p className="text-sm text-slate-500">@{profile.username}</p>
              {profile.bio && (
                <p className="mt-2.5 whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-300">
                  {profile.bio}
                </p>
              )}
              <p className="mt-2.5 text-xs text-slate-500">
                Joined {joinedDate(profile.createdAt)}
              </p>
              <div className="mt-3 flex gap-5 text-sm text-slate-400">
                <span>
                  <span className="font-semibold text-white">{profile.followerCount}</span>{" "}
                  Followers
                </span>
                <span>
                  <span className="font-semibold text-white">{profile.followingCount}</span>{" "}
                  Following
                </span>
              </div>

              {error && <div className="error-banner mt-3 px-3 py-2 text-xs">{error}</div>}

              {editing && <EditProfileForm profile={profile} onSaved={handleSaved} />}
            </div>
          </section>

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

          {activeTab.error && <div className="error-banner">{activeTab.error}</div>}

          {activeTab.loading ? (
            <PostSkeleton />
          ) : activeTab.items.length === 0 ? (
            <div className="glass p-10 text-center text-sm text-slate-400">
              {tab === "Posts"
                ? "No posts yet."
                : tab === "Followers"
                  ? "No followers yet."
                  : "Not following anyone yet."}
            </div>
          ) : (
            <div className="space-y-4">
              {tab === "Posts"
                ? activeTab.items.map((post) => (
                    <PostCard
                      key={post.id}
                      post={post}
                      currentUserId={currentUser?.id}
                      onDeleted={handlePostDeleted}
                    />
                  ))
                : activeTab.items.map((person) => (
                    <UserCard key={person.id} user={person} />
                  ))}
              {!activeTab.last && (
                <button
                  onClick={activeTab.loadMore}
                  disabled={activeTab.loadingMore}
                  className="btn-ghost w-full"
                >
                  {activeTab.loadingMore ? "Loading…" : "Load more"}
                </button>
              )}
            </div>
          )}
        </>
      )}
    </Layout>
  );
}
