import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../../context/AuthContext";

const ADMIN_GROUPS = ["SYSTEM_ADMIN", "SUPER_ADMIN"];

export default function AdminRoute() {
  const { user } = useAuth();
  const location = useLocation();

  const isAdmin = user?.groups?.some((g) => ADMIN_GROUPS.includes(g)) ?? false;

  if (!isAdmin) {
    return <Navigate to="/unauthorized" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
