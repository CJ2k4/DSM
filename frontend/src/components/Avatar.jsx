// Avatar image if the user has one, otherwise their initial in a colored circle.
export default function Avatar({ user, className = "h-9 w-9 text-sm" }) {
  const name = user?.displayName || user?.username || "?";

  if (user?.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt=""
        className={`${className} shrink-0 rounded-full object-cover`}
      />
    );
  }

  return (
    <div
      className={`${className} flex shrink-0 items-center justify-center rounded-full bg-blue-100 font-semibold text-blue-700`}
    >
      {name.charAt(0).toUpperCase()}
    </div>
  );
}
