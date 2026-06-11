import { useState } from "react";
import Navbar from "../components/Navbar";
import UserCard from "../components/UserCard";
import { searchUsers } from "../api/users";
import { extractErrorMessage } from "../api/client";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState(null); // null = not searched yet
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(true);
  const [searching, setSearching] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  async function loadPage(q, pageNumber) {
    const data = await searchUsers(q, pageNumber);
    setResults((prev) =>
      pageNumber === 0 ? data.content : [...prev, ...data.content]
    );
    setPage(data.number);
    setLast(data.last);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const q = query.trim();
    if (!q || searching) return;
    setSearching(true);
    setError(null);
    try {
      await loadPage(q, 0);
    } catch (err) {
      setError(extractErrorMessage(err, "Search failed"));
    } finally {
      setSearching(false);
    }
  }

  async function handleLoadMore() {
    setLoadingMore(true);
    try {
      await loadPage(query.trim(), page + 1);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not load more results"));
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-2xl space-y-4 px-4 py-6">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <label htmlFor="user-search" className="sr-only">
            Search users
          </label>
          <input
            id="user-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search people by name or username…"
            maxLength={80}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <button
            type="submit"
            disabled={searching || !query.trim()}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
          >
            {searching ? "Searching…" : "Search"}
          </button>
        </form>

        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}

        {results === null ? (
          <div className="py-10 text-center text-sm text-slate-400">
            Find people to follow.
          </div>
        ) : results.length === 0 ? (
          <div className="rounded-2xl bg-white p-8 text-center text-sm text-slate-500 shadow-sm">
            No users found for “{query.trim()}”.
          </div>
        ) : (
          <div className="space-y-3">
            {results.map((person) => (
              <UserCard key={person.id} user={person} />
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
          </div>
        )}
      </main>
    </div>
  );
}
