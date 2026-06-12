// Compact relative time formatter, e.g. "just now", "5m", "3h", "2d", or a date.
export function relativeTime(isoString) {
  if (!isoString) return "";
  const then = new Date(isoString).getTime();
  const seconds = Math.floor((Date.now() - then) / 1000);

  if (seconds < 45) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d`;

  return new Date(isoString).toLocaleDateString();
}
