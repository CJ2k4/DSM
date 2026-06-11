import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import Avatar from "../components/Avatar";
import PostCard from "../components/PostCard";
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
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-2xl space-y-4 px-4 py-6">
        {loading ? (
          <div className="py-10 text-center text-slate-400">Loading profile…</div>
        ) : error && !profile ? (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        ) : (
          <>
            <section className="overflow-hidden rounded-2xl bg-white shadow-sm">
              {profile.bannerUrl ? (
                <img src={profile.bannerUrl} alt="" className="h-32 w-full object-cover" />
              ) : (
                <div className="h-20 bg-gradient-to-r from-blue-100 to-indigo-100" />
              )}
              <div className="p-4">
                <div className="-mt-12 mb-3 flex items-end justify-between">
                  <div className="rounded-full ring-4 ring-white">
                    <Avatar user={profile} className="h-20 w-20 text-2xl" />
                  </div>
                  {profile.ownProfile ? (
                    <button
                      onClick={() => setEditing((open) => !open)}
                      className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                    >
                      {editing ? "Cancel" : "Edit profile"}
                    </button>
                  ) : (
                    <button
                      onClick={toggleFollow}
                      disabled={followBusy}
                      className={`rounded-lg px-4 py-1.5 text-sm font-medium transition disabled:opacity-60 ${
                        profile.following
                          ? "border border-slate-300 text-slate-600 hover:bg-slate-100"
                          : "bg-blue-600 text-white hover:bg-blue-700"
                      }`}
                    >
                      {profile.following ? "Unfollow" : "Follow"}
                    </button>
                  )}
                </div>

                <h1 className="text-xl font-semibold text-slate-900">
                  {profile.displayName}
                </h1>
                <p className="text-sm text-slate-400">@{profile.username}</p>
                {profile.bio && (
                  <p className="mt-2 whitespace-pre-wrap break-words text-sm text-slate-700">
                    {profile.bio}
                  </p>
                )}
                <p className="mt-2 text-xs text-slate-400">
                  Joined {joinedDate(profile.createdAt)}
                </p>
                <div className="mt-3 flex gap-4 text-sm text-slate-600">
                  <span>
                    <span className="font-semibold text-slate-900">
                      {profile.followerCount}
                    </span>{" "}
                    Followers
                  </span>
                  <span>
                    <span className="font-semibold text-slate-900">
                      {profile.followingCount}
                    </span>{" "}
                    Following
                  </span>
                </div>

                {error && (
                  <div className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">
                    {error}
                  </div>
                )}

                {editing && (
                  <EditProfileForm profile={profile} onSaved={handleSaved} />
                )}
              </div>
            </section>

            <nav className="flex rounded-xl bg-white p-1 shadow-sm">
              {TABS.map((name) => (
                <button
                  key={name}
                  onClick={() => setTab(name)}
                  className={`flex-1 rounded-lg py-2 text-sm font-medium transition ${
                    tab === name
                      ? "bg-blue-600 text-white"
                      : "text-slate-500 hover:bg-slate-50"
                  }`}
                >
                  {name}
                </button>
              ))}
            </nav>

            {activeTab.error && (
              <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                {activeTab.error}
              </div>
            )}

            {activeTab.loading ? (
              <div className="py-10 text-center text-slate-400">Loading…</div>
            ) : activeTab.items.length === 0 ? (
              <div className="rounded-2xl bg-white p-8 text-center text-sm text-slate-500 shadow-sm">
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
                    className="w-full rounded-lg border border-slate-300 bg-white py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-50 disabled:opacity-60"
                  >
                    {activeTab.loadingMore ? "Loading…" : "Load more"}
                  </button>
                )}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
