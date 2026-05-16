import { Navigate, Outlet, useLocation } from "react-router";
import { useAuth } from "../../context/AuthContext";
import { decodeJwtGroups } from "../../utils/jwt";

const ADMIN_GROUPS = new Set(["SYSTEM_ADMIN", "SUPER_ADMIN"]);

export default function AdminRoute() {
  const { accessToken } = useAuth();
  const location = useLocation();

  // Derive groups from the signed JWT claim — not from the mutable user object
  // in storage, which a client could tamper with.
  const groups = accessToken ? decodeJwtGroups(accessToken) : [];
  const isAdmin = groups.some((g) => ADMIN_GROUPS.has(g));

  if (!isAdmin) {
    return <Navigate to="/unauthorized" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
