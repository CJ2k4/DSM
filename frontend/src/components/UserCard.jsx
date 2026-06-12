import { Link } from "react-router-dom";
import Avatar from "./Avatar";

// Compact user row used in search results and follower/following lists.
export default function UserCard({ user }) {
  return (
    <Link
      to={`/profile/${user.username}`}
      className="glass glass-hover flex items-center gap-3 p-4"
    >
      <Avatar user={user} className="h-12 w-12 text-base" />
      <div className="min-w-0 leading-tight">
        <div className="truncate text-sm font-semibold text-white">
          {user.displayName}
        </div>
        <div className="text-xs text-slate-500">@{user.username}</div>
        {user.bio && (
          <p className="mt-1 truncate text-xs text-slate-400">{user.bio}</p>
        )}
      </div>
    </Link>
  );
}
