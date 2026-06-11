import { useState } from "react";
import { createPost } from "../api/posts";
import { extractErrorMessage } from "../api/client";

const MAX_CONTENT = 1000;

export default function CreatePostForm({ onCreated }) {
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
    <form onSubmit={handleSubmit} className="rounded-2xl bg-white p-4 shadow-sm">
      {error && (
        <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="What's happening?"
        rows={3}
        maxLength={MAX_CONTENT}
        className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
      />
      {showImage && (
        <input
          type="url"
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
          placeholder="Image URL (optional)"
          className="mt-2 w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      )}
      <div className="mt-2 flex items-center justify-between">
        <div className="flex items-center gap-3 text-xs text-slate-400">
          <button
            type="button"
            onClick={() => setShowImage((v) => !v)}
            className="font-medium text-slate-500 hover:text-blue-600"
          >
            {showImage ? "Remove image" : "Add image"}
          </button>
          <span>
            {content.length}/{MAX_CONTENT}
          </span>
        </div>
        <button
          type="submit"
          disabled={submitting || !content.trim()}
          className="rounded-lg bg-blue-600 px-4 py-1.5 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? "Posting…" : "Post"}
        </button>
      </div>
    </form>
  );
}
