// Shimmering placeholder card shown while posts load.
export default function PostSkeleton() {
  return (
    <div className="glass p-4">
      <div className="mb-3 flex items-center gap-2">
        <div className="skeleton h-9 w-9 rounded-full" />
        <div className="space-y-1.5">
          <div className="skeleton h-3 w-28" />
          <div className="skeleton h-2.5 w-20" />
        </div>
      </div>
      <div className="space-y-2">
        <div className="skeleton h-3 w-full" />
        <div className="skeleton h-3 w-4/5" />
        <div className="skeleton h-3 w-2/5" />
      </div>
    </div>
  );
}
