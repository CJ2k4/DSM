import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useRealtime } from "../store/RealtimeContext";
import Avatar from "./Avatar";
import NotificationBell from "./NotificationBell";
import { FeedIcon, LogoMark, LogoutIcon, NetworkIcon, SearchIcon } from "./icons";

function navLinkClass({ isActive }) {
  return `flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium transition ${
    isActive ? "bg-white/10 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"
  }`;
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const { presence } = useRealtime();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className="sticky top-0 z-10 border-b border-white/[0.06] bg-[#07070b]/70 backdrop-blur-xl">
      <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-2 sm:gap-4">
          <Link to="/feed" className="flex items-center gap-2">
            <LogoMark className="h-6 w-6" />
            <span className="brand-text font-display text-lg font-bold tracking-tight">
              DSM
            </span>
          </Link>
          <nav className="flex items-center gap-1">
            <NavLink to="/feed" className={navLinkClass}>
              <FeedIcon className="h-4 w-4" />
              <span className="hidden sm:inline">Feed</span>
            </NavLink>
            <NavLink to="/search" className={navLinkClass}>
              <SearchIcon className="h-4 w-4" />
              <span className="hidden sm:inline">Search</span>
            </NavLink>
            <NavLink to="/federation" className={navLinkClass}>
              <NetworkIcon className="h-4 w-4" />
              <span className="hidden sm:inline">Network</span>
            </NavLink>
          </nav>
        </div>

        <div className="flex items-center gap-2">
          {presence.count > 0 && (
            <span
              title={presence.usernames.join(", ")}
              className="hidden items-center gap-1.5 rounded-full border border-emerald-400/20 bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-300 md:flex"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.8)]" />
              {presence.count} online
            </span>
          )}
          <NotificationBell />
          {user && (
            <Link
              to={`/profile/${user.username}`}
              className="group flex items-center gap-2 rounded-full border border-transparent py-1 pl-1 pr-3 transition hover:border-white/10 hover:bg-white/5"
            >
              <Avatar user={user} className="h-7 w-7 text-xs" />
              <span className="hidden text-sm text-slate-300 transition group-hover:text-white sm:inline">
                {user.displayName}
              </span>
            </Link>
          )}
          <button
            onClick={handleLogout}
            title="Logout"
            className="rounded-lg p-2 text-slate-500 transition hover:bg-white/5 hover:text-white"
          >
            <LogoutIcon className="h-[18px] w-[18px]" />
            <span className="sr-only">Logout</span>
          </button>
        </div>
      </div>
    </header>
  );
}
