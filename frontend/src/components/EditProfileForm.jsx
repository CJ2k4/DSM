import { useState } from "react";
import { updateProfile } from "../api/users";
import { extractErrorMessage } from "../api/client";

// Inline form on the own-profile page for displayName / bio / avatar / banner.
export default function EditProfileForm({ profile, onSaved }) {
  const [form, setForm] = useState({
    displayName: profile.displayName || "",
    bio: profile.bio || "",
    avatarUrl: profile.avatarUrl || "",
    bannerUrl: profile.bannerUrl || "",
  });
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  function update(field) {
    return (event) => setForm((prev) => ({ ...prev, [field]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const updated = await updateProfile({
        displayName: form.displayName.trim() || null,
        bio: form.bio.trim() || null,
        avatarUrl: form.avatarUrl.trim() || null,
        bannerUrl: form.bannerUrl.trim() || null,
      });
      onSaved(updated);
    } catch (err) {
      setError(extractErrorMessage(err, "Could not update your profile"));
    } finally {
      setSaving(false);
    }
  }

  const inputClass =
    "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500";

  return (
    <form onSubmit={handleSubmit} className="mt-4 space-y-3 border-t border-slate-100 pt-4">
      {error && (
        <div className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{error}</div>
      )}
      <div>
        <label htmlFor="edit-display-name" className="mb-1 block text-sm font-medium text-slate-700">
          Display name
        </label>
        <input
          id="edit-display-name"
          type="text"
          value={form.displayName}
          onChange={update("displayName")}
          maxLength={80}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor="edit-bio" className="mb-1 block text-sm font-medium text-slate-700">
          Bio
        </label>
        <textarea
          id="edit-bio"
          value={form.bio}
          onChange={update("bio")}
          maxLength={500}
          rows={3}
          className={`${inputClass} resize-none`}
        />
      </div>
      <div>
        <label htmlFor="edit-avatar-url" className="mb-1 block text-sm font-medium text-slate-700">
          Avatar URL
        </label>
        <input
          id="edit-avatar-url"
          type="url"
          value={form.avatarUrl}
          onChange={update("avatarUrl")}
          maxLength={2048}
          placeholder="https://…"
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor="edit-banner-url" className="mb-1 block text-sm font-medium text-slate-700">
          Banner URL
        </label>
        <input
          id="edit-banner-url"
          type="url"
          value={form.bannerUrl}
          onChange={update("bannerUrl")}
          maxLength={2048}
          placeholder="https://…"
          className={inputClass}
        />
      </div>
      <button
        type="submit"
        disabled={saving}
        className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
      >
        {saving ? "Saving…" : "Save changes"}
      </button>
    </form>
  );
}
