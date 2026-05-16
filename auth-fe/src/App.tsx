import { Navigate, Route, BrowserRouter as Router, Routes } from "react-router";
import AdminRoute from "./components/common/AdminRoute";
import ProtectedRoute from "./components/common/ProtectedRoute";
import { ScrollToTop } from "./components/common/ScrollToTop";
import { AuthProvider, useAuth } from "./context/AuthContext";
import AppLayout from "./layout/AppLayout";
import AuditPage from "./pages/Audit/AuditPage";
import AdCallback from "./pages/AuthPages/AdCallback";
import ForgotPassword from "./pages/AuthPages/ForgotPassword";
import OtpVerify from "./pages/AuthPages/OtpVerify";
import ResetPassword from "./pages/AuthPages/ResetPassword";
import SignIn from "./pages/AuthPages/SignIn";
import SignUp from "./pages/AuthPages/SignUp";
import Home from "./pages/Dashboard/Home";
import GroupDetailPage from "./pages/Groups/GroupDetailPage";
import GroupsPage from "./pages/Groups/GroupsPage";
import NotFound from "./pages/OtherPage/NotFound";
import Unauthorized from "./pages/OtherPage/Unauthorized";
import PermissionsPage from "./pages/Permissions/PermissionsPage";
import RoleDetailPage from "./pages/Roles/RoleDetailPage";
import RolesPage from "./pages/Roles/RolesPage";
import SettingsPage from "./pages/Settings/SettingsPage";
import UserProfiles from "./pages/UserProfiles";
import UserDetailPage from "./pages/Users/UserDetailPage";
import UsersPage from "./pages/Users/UsersPage";

function RootRedirect() {
  const { isAuthenticated, rehydrating } = useAuth();
  if (rehydrating) return null; // wait for silent refresh before redirecting
  return <Navigate to={isAuthenticated ? "/dashboard" : "/signin"} replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <ScrollToTop />
        <Routes>
          {/* Root — redirects to /signin or /dashboard based on auth state */}
          <Route path="/" element={<RootRedirect />} />

          {/* Protected routes — require authentication */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              {/* Admin-only routes — require SYSTEM_ADMIN or SUPER_ADMIN group */}
              <Route element={<AdminRoute />}>
                <Route path="/dashboard" element={<Home />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/users/:id" element={<UserDetailPage />} />
                <Route path="/groups" element={<GroupsPage />} />
                <Route path="/groups/:id" element={<GroupDetailPage />} />
                <Route path="/roles" element={<RolesPage />} />
                <Route path="/roles/:id" element={<RoleDetailPage />} />
                <Route path="/permissions" element={<PermissionsPage />} />
                <Route path="/audit" element={<AuditPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
              {/* Accessible to any authenticated user */}
              <Route path="/profile" element={<UserProfiles />} />
              {/* <Route path="/calendar" element={<Calendar />} />
              <Route path="/blank" element={<Blank />} />
              <Route path="/form-elements" element={<FormElements />} />
              <Route path="/basic-tables" element={<BasicTables />} />
              <Route path="/alerts" element={<Alerts />} />
              <Route path="/avatars" element={<Avatars />} />
              <Route path="/badge" element={<Badges />} />
              <Route path="/buttons" element={<Buttons />} />
              <Route path="/images" element={<Images />} />
              <Route path="/videos" element={<Videos />} />
              <Route path="/line-chart" element={<LineChart />} />
              <Route path="/bar-chart" element={<BarChart />} /> */}
            </Route>
          </Route>

          {/* Auth routes (public) */}
          <Route path="/signin" element={<SignIn />} />
          <Route path="/signup" element={<SignUp />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verify-otp" element={<OtpVerify />} />
          <Route path="/auth/callback" element={<AdCallback />} />

          {/* 403 — authenticated but not in an admin group */}
          <Route path="/unauthorized" element={<Unauthorized />} />

          {/* Fallback */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}
