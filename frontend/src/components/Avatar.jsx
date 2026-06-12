// Gradient pairs cycled deterministically per username, so every user gets a
// stable, distinct fallback avatar.
const GRADIENTS = [
  "from-indigo-500 to-violet-500",
  "from-cyan-500 to-blue-500",
  "from-fuchsia-500 to-pink-500",
  "from-amber-500 to-orange-600",
  "from-emerald-500 to-teal-500",
  "from-rose-500 to-red-500",
];

function gradientFor(seed) {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  return GRADIENTS[Math.abs(hash) % GRADIENTS.length];
}

// Avatar image if the user has one, otherwise their initial on a per-user gradient.
export default function Avatar({ user, className = "h-9 w-9 text-sm" }) {
  const name = user?.displayName || user?.username || "?";

  if (user?.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt=""
        className={`${className} shrink-0 rounded-full object-cover ring-1 ring-white/15`}
      />
    );
  }

  return (
    <div
      className={`${className} flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br ${gradientFor(
        user?.username || name
      )} font-display font-semibold text-white ring-1 ring-white/15`}
    >
      {name.charAt(0).toUpperCase()}
    </div>
  );
}
