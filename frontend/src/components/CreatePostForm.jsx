import { useState } from "react";
import { createPost } from "../api/posts";
import { extractErrorMessage } from "../api/client";
import { useAuth } from "../hooks/useAuth";
import Avatar from "./Avatar";
import { ImageIcon } from "./icons";

const MAX_CONTENT = 1000;

export default function CreatePostForm({ onCreated }) {
  const { user } = useAuth();
  const [content, setContent] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [showImage, setShowImage] = useState(false);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!content.trim()) return;
    setError(null);
    setSubmitting(true);
    try {
      const post = await createPost({ content: content.trim(), imageUrl: imageUrl.trim() });
      setContent("");
      setImageUrl("");
      setShowImage(false);
      onCreated?.(post);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not publish post"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="glass p-4">
      {error && <div className="error-banner mb-3 px-3 py-2">{error}</div>}
      <div className="flex items-start gap-3">
        <Avatar user={user} className="h-9 w-9 text-sm" />
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="What's happening?"
          rows={3}
          maxLength={MAX_CONTENT}
          className="w-full resize-none border-0 bg-transparent text-[15px] leading-relaxed text-white placeholder-slate-500 focus:outline-none focus:ring-0"
        />
      </div>
      {showImage && (
        <input
          type="url"
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
          placeholder="Image URL (optional)"
          className="field mt-2"
        />
      )}
      <div className="mt-2 flex items-center justify-between border-t border-white/[0.06] pt-3">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setShowImage((v) => !v)}
            title={showImage ? "Remove image" : "Add image"}
            className={`rounded-lg p-2 transition ${
              showImage
                ? "bg-violet-500/10 text-violet-300"
                : "text-slate-500 hover:bg-white/5 hover:text-violet-300"
            }`}
          >
            <ImageIcon />
            <span className="sr-only">{showImage ? "Remove image" : "Add image"}</span>
          </button>
          <span
            className={`text-xs tabular-nums ${
              content.length > MAX_CONTENT - 50 ? "text-amber-400" : "text-slate-600"
            }`}
          >
            {content.length}/{MAX_CONTENT}
          </span>
        </div>
        <button
          type="submit"
          disabled={submitting || !content.trim()}
          className="btn-primary px-5 py-1.5"
        >
          {submitting ? "Posting…" : "Post"}
        </button>
      </div>
    </form>
  );
}
