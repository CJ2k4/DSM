// Fixed full-screen backdrop: slow-drifting gradient blobs over a faint dot grid.
export default function Aurora() {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="absolute -left-40 -top-40 h-[34rem] w-[34rem] animate-aurora-slow rounded-full bg-indigo-600/20 blur-[120px]" />
      <div className="absolute -right-40 top-1/4 h-[30rem] w-[30rem] animate-aurora-slower rounded-full bg-fuchsia-600/[0.13] blur-[120px]" />
      <div className="absolute -bottom-48 left-1/3 h-[36rem] w-[36rem] animate-aurora-slow rounded-full bg-cyan-500/10 blur-[130px]" />
      <div className="absolute inset-0 bg-[radial-gradient(rgba(255,255,255,0.05)_1px,transparent_1px)] [background-size:28px_28px] [mask-image:radial-gradient(ellipse_at_center,black_30%,transparent_75%)]" />
    </div>
  );
}
