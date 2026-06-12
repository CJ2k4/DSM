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

  return (
    <form
      onSubmit={handleSubmit}
      className="mt-4 animate-fade-up space-y-3 border-t border-white/[0.06] pt-4"
    >
      {error && <div className="error-banner px-3 py-2 text-xs">{error}</div>}
      <div>
        <label htmlFor="edit-display-name" className="field-label">
          Display name
        </label>
        <input
          id="edit-display-name"
          type="text"
          value={form.displayName}
          onChange={update("displayName")}
          maxLength={80}
          className="field"
        />
      </div>
      <div>
        <label htmlFor="edit-bio" className="field-label">
          Bio
        </label>
        <textarea
          id="edit-bio"
          value={form.bio}
          onChange={update("bio")}
          maxLength={500}
          rows={3}
          className="field resize-none"
        />
      </div>
      <div>
        <label htmlFor="edit-avatar-url" className="field-label">
          Avatar URL
        </label>
        <input
          id="edit-avatar-url"
          type="url"
          value={form.avatarUrl}
          onChange={update("avatarUrl")}
          maxLength={2048}
          placeholder="https://…"
          className="field"
        />
      </div>
      <div>
        <label htmlFor="edit-banner-url" className="field-label">
          Banner URL
        </label>
        <input
          id="edit-banner-url"
          type="url"
          value={form.bannerUrl}
          onChange={update("bannerUrl")}
          maxLength={2048}
          placeholder="https://…"
          className="field"
        />
      </div>
      <button type="submit" disabled={saving} className="btn-primary w-full">
        {saving ? "Saving…" : "Save changes"}
      </button>
    </form>
  );
}
