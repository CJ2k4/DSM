import { useEffect, useState } from "react";
import Layout from "../components/Layout";
import { addServer, getServers, removeServer, syncNow } from "../api/federation";
import { extractErrorMessage } from "../api/client";
import { relativeTime } from "../utils/time";
import { NetworkIcon, TrashIcon } from "../components/icons";

function StatusDot({ status }) {
  const active = status === "ACTIVE";
  return (
    <span className="flex items-center gap-1.5 text-xs font-medium">
      <span
        className={`h-2 w-2 rounded-full ${
          active ? "bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.8)]" : "bg-rose-400"
        }`}
      />
      <span className={active ? "text-emerald-300" : "text-rose-300"}>
        {active ? "Active" : "Unreachable"}
      </span>
    </span>
  );
}

export default function FederationPage() {
  const [servers, setServers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [baseUrl, setBaseUrl] = useState("");
  const [adding, setAdding] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncResults, setSyncResults] = useState(null);

  async function refresh() {
    setServers(await getServers());
  }

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const data = await getServers();
        if (active) setServers(data);
      } catch (err) {
        if (active) setError(extractErrorMessage(err, "Could not load peer servers"));
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  async function handleAdd(event) {
    event.preventDefault();
    const url = baseUrl.trim();
    if (!url || adding) return;
    setAdding(true);
    setError(null);
    try {
      await addServer(url);
      setBaseUrl("");
      await refresh();
    } catch (err) {
      setError(extractErrorMessage(err, "Could not add this server"));
    } finally {
      setAdding(false);
    }
  }

  async function handleSync() {
    if (syncing) return;
    setSyncing(true);
    setError(null);
    setSyncResults(null);
    try {
      const results = await syncNow();
      setSyncResults(results);
      await refresh();
    } catch (err) {
      setError(extractErrorMessage(err, "Sync failed"));
    } finally {
      setSyncing(false);
    }
  }

  async function handleRemove(id) {
    setError(null);
    try {
      await removeServer(id);
      setServers((prev) => prev.filter((s) => s.id !== id));
    } catch (err) {
      setError(extractErrorMessage(err, "Could not remove this server"));
    }
  }

  return (
    <Layout>
      <div className="glass p-5">
        <div className="flex items-center gap-2.5">
          <NetworkIcon className="h-5 w-5 text-violet-300" />
          <h1 className="font-display text-lg font-bold tracking-tight text-white">
            Federated Network
          </h1>
        </div>
        <p className="mt-1.5 text-sm text-slate-400">
          Connect this server to other DSM nodes. Their public posts appear in your
          Global feed — and yours in theirs.
        </p>

        <form onSubmit={handleAdd} className="mt-4 flex gap-2">
          <label htmlFor="peer-url" className="sr-only">
            Peer server URL
          </label>
          <input
            id="peer-url"
            type="url"
            value={baseUrl}
            onChange={(e) => setBaseUrl(e.target.value)}
            placeholder="https://other-server.example or http://localhost:8081"
            maxLength={512}
            className="field"
          />
          <button type="submit" disabled={adding || !baseUrl.trim()} className="btn-primary shrink-0">
            {adding ? "Connecting…" : "Add server"}
          </button>
        </form>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="flex items-center justify-between">
        <h2 className="text-sm font-medium uppercase tracking-wider text-slate-500">
          Peers ({servers.length})
        </h2>
        <button
          onClick={handleSync}
          disabled={syncing || servers.length === 0}
          className="btn-ghost px-3 py-1.5 text-xs"
        >
          {syncing ? "Syncing…" : "Sync now"}
        </button>
      </div>

      {syncResults && (
        <div className="glass animate-fade-up p-3 text-xs text-slate-400">
          {syncResults.map((r) => (
            <div key={r.baseUrl} className="flex items-center justify-between py-1">
              <span className="text-slate-300">{r.name}</span>
              <span>
                {r.status === "ACTIVE"
                  ? `pulled ${r.fetched} new post${r.fetched === 1 ? "" : "s"}`
                  : "unreachable"}
              </span>
            </div>
          ))}
        </div>
      )}

      {loading ? (
        <div className="glass p-6">
          <div className="skeleton h-5 w-2/3" />
          <div className="skeleton mt-2 h-4 w-1/3" />
        </div>
      ) : servers.length === 0 ? (
        <div className="glass p-10 text-center">
          <NetworkIcon className="mx-auto h-8 w-8 text-slate-600" />
          <p className="mt-3 font-display text-lg font-semibold text-white">
            No peers yet
          </p>
          <p className="mt-1 text-sm text-slate-400">
            Add another DSM server above to start federating.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {servers.map((server) => (
            <div key={server.id} className="glass glass-hover flex items-center justify-between p-4">
              <div className="min-w-0">
                <div className="flex items-center gap-3">
                  <span className="truncate text-sm font-semibold text-white">
                    {server.name}
                  </span>
                  <StatusDot status={server.status} />
                </div>
                <div className="mt-0.5 truncate text-xs text-slate-500">
                  {server.baseUrl}
                  {" · "}
                  {server.remotePostCount} post{server.remotePostCount === 1 ? "" : "s"} synced
                  {server.lastSyncAt && ` · last sync ${relativeTime(server.lastSyncAt)}`}
                </div>
              </div>
              <button
                onClick={() => handleRemove(server.id)}
                title="Remove server"
                className="ml-3 shrink-0 rounded-lg p-2 text-slate-600 transition hover:bg-rose-500/10 hover:text-rose-400"
              >
                <TrashIcon />
                <span className="sr-only">Remove</span>
              </button>
            </div>
          ))}
        </div>
      )}
    </Layout>
  );
}
