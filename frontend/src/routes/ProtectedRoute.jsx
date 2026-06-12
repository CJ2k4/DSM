import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { LogoMark } from "../components/icons";

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3">
        <LogoMark className="h-10 w-10 animate-pulse" />
        <span className="brand-text font-display text-sm font-semibold tracking-widest">
          LOADING
        </span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
