import { useState } from "react";
import Layout from "../components/Layout";
import UserCard from "../components/UserCard";
import { searchUsers } from "../api/users";
import { extractErrorMessage } from "../api/client";
import { SearchIcon } from "../components/icons";

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
    <Layout>
      <form onSubmit={handleSubmit} className="flex gap-2">
        <label htmlFor="user-search" className="sr-only">
          Search users
        </label>
        <div className="relative w-full">
          <SearchIcon className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            id="user-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search people by name or username…"
            maxLength={80}
            className="field pl-10"
          />
        </div>
        <button
          type="submit"
          disabled={searching || !query.trim()}
          className="btn-primary shrink-0"
        >
          {searching ? "Searching…" : "Search"}
        </button>
      </form>

      {error && <div className="error-banner">{error}</div>}

      {results === null ? (
        <div className="glass p-12 text-center">
          <SearchIcon className="mx-auto h-8 w-8 text-slate-600" />
          <p className="mt-3 font-display text-lg font-semibold text-white">
            Find your people
          </p>
          <p className="mt-1 text-sm text-slate-400">
            Search across the network by name or username.
          </p>
        </div>
      ) : results.length === 0 ? (
        <div className="glass p-10 text-center text-sm text-slate-400">
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
              className="btn-ghost w-full"
            >
              {loadingMore ? "Loading…" : "Load more"}
            </button>
          )}
        </div>
      )}
    </Layout>
  );
}
