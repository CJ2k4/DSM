import { LogoMark } from "./icons";

// Split-screen auth layout: brand statement panel (lg+) beside the form card.
export default function AuthShell({ children }) {
  return (
    <div className="flex min-h-screen">
      {/* Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden border-r border-white/[0.06] p-12 lg:flex lg:w-1/2">
        <div className="flex items-center gap-2.5">
          <LogoMark className="h-8 w-8" />
          <span className="brand-text font-display text-2xl font-bold tracking-tight">
            DSM
          </span>
        </div>

        <div>
          <h2 className="font-display text-5xl font-bold leading-[1.1] tracking-tight text-white xl:text-6xl">
            Your network.
            <br />
            <span className="brand-text">Your rules.</span>
          </h2>
          <p className="mt-6 max-w-md text-base leading-relaxed text-slate-400">
            A federated social platform. No single server owns your voice — post,
            follow, and connect across an open network of communities.
          </p>
        </div>

        <ul className="space-y-2.5 text-sm text-slate-500">
          <li className="flex items-center gap-2.5">
            <span className="h-1.5 w-1.5 rounded-full bg-indigo-400" />
            Own and export your data, any time
          </li>
          <li className="flex items-center gap-2.5">
            <span className="h-1.5 w-1.5 rounded-full bg-violet-400" />
            Community servers — your college, your city
          </li>
          <li className="flex items-center gap-2.5">
            <span className="h-1.5 w-1.5 rounded-full bg-cyan-400" />
            Federated feeds across independent nodes
          </li>
        </ul>
      </div>

      {/* Form panel */}
      <div className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-md animate-fade-up">
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <LogoMark className="h-7 w-7" />
            <span className="brand-text font-display text-xl font-bold tracking-tight">
              DSM
            </span>
          </div>
          {children}
        </div>
      </div>
    </div>
  );
}
