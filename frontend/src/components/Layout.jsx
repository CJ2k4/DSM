import Navbar from "./Navbar";

// Standard authenticated-page shell: navbar + centered column.
export default function Layout({ children }) {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-2xl animate-fade-up space-y-4 px-4 py-6">
        {children}
      </main>
    </div>
  );
}
