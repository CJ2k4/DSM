import Avatar from "./Avatar";
import { relativeTime } from "../utils/time";
import { NetworkIcon } from "./icons";

// A post pulled from a federated peer. Read-only: likes/comments live on the
// origin server, so there are no interaction buttons.
export default function RemotePostCard({ post }) {
  return (
    <article className="glass glass-hover p-4">
      <div className="mb-2.5 flex items-center justify-between gap-2">
        <div className="flex items-center gap-2.5">
          <Avatar user={post.author} className="h-9 w-9 text-sm" />
          <div className="leading-tight">
            <div className="text-sm font-semibold text-white">
              {post.author?.displayName}
            </div>
            <div className="text-xs text-slate-500">
              @{post.author?.username} · {relativeTime(post.createdAt)}
            </div>
          </div>
        </div>
        <span
          title={post.server?.baseUrl}
          className="flex shrink-0 items-center gap-1.5 rounded-full border border-violet-400/20 bg-violet-500/10 px-2.5 py-1 text-xs font-medium text-violet-300"
        >
          <NetworkIcon className="h-3.5 w-3.5" />
          via {post.server?.name}
        </span>
      </div>

      <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed text-slate-200">
        {post.content}
      </p>

      {post.imageUrl && (
        <img
          src={post.imageUrl}
          alt=""
          className="mt-3 max-h-96 w-full rounded-xl object-cover ring-1 ring-white/10"
        />
      )}
    </article>
  );
}
