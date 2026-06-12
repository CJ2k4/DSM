// Minimal inline SVG icon set (stroke style, inherits currentColor).
const base = {
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round",
  strokeLinejoin: "round",
  viewBox: "0 0 24 24",
};

export function LogoMark({ className = "h-6 w-6" }) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none">
      <defs>
        <linearGradient id="dsm-logo-g" x1="0" y1="0" x2="24" y2="24">
          <stop offset="0%" stopColor="#818cf8" />
          <stop offset="50%" stopColor="#c084fc" />
          <stop offset="100%" stopColor="#22d3ee" />
        </linearGradient>
      </defs>
      <circle cx="12" cy="12" r="4" fill="url(#dsm-logo-g)" />
      <ellipse
        cx="12"
        cy="12"
        rx="10"
        ry="4.5"
        stroke="url(#dsm-logo-g)"
        strokeWidth="1.5"
        transform="rotate(-24 12 12)"
      />
      <circle cx="20.5" cy="8" r="1.6" fill="#22d3ee" />
    </svg>
  );
}

export function HeartIcon({ filled = false, className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className} fill={filled ? "currentColor" : "none"}>
      <path d="M19.5 12.572 12 20l-7.5-7.428A5 5 0 1 1 12 6.006a5 5 0 1 1 7.5 6.566Z" />
    </svg>
  );
}

export function CommentIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" />
    </svg>
  );
}

export function SearchIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <circle cx="11" cy="11" r="7" />
      <path d="m21 21-4.35-4.35" />
    </svg>
  );
}

export function TrashIcon({ className = "h-4 w-4" }) {
  return (
    <svg {...base} className={className}>
      <path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
    </svg>
  );
}

export function ImageIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <rect x="3" y="3" width="18" height="18" rx="3" />
      <circle cx="9" cy="9" r="1.8" />
      <path d="m21 15-4.586-4.586a2 2 0 0 0-2.828 0L5 19" />
    </svg>
  );
}

export function LogoutIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" />
    </svg>
  );
}

export function BellIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0" />
    </svg>
  );
}

export function NetworkIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <circle cx="12" cy="5" r="2.2" />
      <circle cx="5" cy="18" r="2.2" />
      <circle cx="19" cy="18" r="2.2" />
      <path d="M10.9 6.9 6.2 16M13.1 6.9l4.7 9.1M7.2 18h9.6" />
    </svg>
  );
}

export function FeedIcon({ className = "h-[18px] w-[18px]" }) {
  return (
    <svg {...base} className={className}>
      <path d="m3 11 9-8 9 8M5 9.5V19a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9.5" />
    </svg>
  );
}
