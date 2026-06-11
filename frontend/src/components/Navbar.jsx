import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-4">
          <Link to="/feed" className="text-lg font-semibold text-slate-900">
            DSM 🌐
          </Link>
          <Link
            to="/search"
            className="text-sm font-medium text-slate-500 transition hover:text-slate-900"
          >
            Search
          </Link>
        </div>
        <div className="flex items-center gap-3">
          {user && (
            <Link
              to={`/profile/${user.username}`}
              className="text-sm text-slate-600 transition hover:text-slate-900"
            >
              {user.displayName}{" "}
              <span className="text-slate-400">@{user.username}</span>
            </Link>
          )}
          <button
            onClick={handleLogout}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
          >
            Logout
          </button>
        </div>
      </div>
    </header>
  );
}
