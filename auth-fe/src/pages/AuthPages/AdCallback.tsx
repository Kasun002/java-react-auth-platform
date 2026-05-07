import { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { useAuth } from "../../context/AuthContext";
import { adLogin } from "../../services/authService";

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL;
const REALM = import.meta.env.VITE_KEYCLOAK_REALM;
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;

export default function AdCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;

    const code = searchParams.get("code");
    const error = searchParams.get("error");

    if (error) {
      navigate("/signin?error=ad_cancelled", { replace: true });
      return;
    }

    if (!code) {
      navigate("/signin?error=ad_no_code", { replace: true });
      return;
    }

    handled.current = true;

    const verifier = sessionStorage.getItem("pkce_verifier");
    if (!verifier) {
      navigate("/signin?error=ad_no_verifier", { replace: true });
      return;
    }
    sessionStorage.removeItem("pkce_verifier");

    const redirectUri = `${window.location.origin}/auth/callback`;
    const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

    const body = new URLSearchParams({
      grant_type: "authorization_code",
      client_id: CLIENT_ID,
      redirect_uri: redirectUri,
      code,
      code_verifier: verifier,
    });

    fetch(tokenUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString(),
    })
      .then((res) => {
        if (!res.ok) throw new Error("Token exchange failed");
        return res.json();
      })
      .then(async (tokens) => {
        const idToken: string = tokens.id_token;
        if (!idToken) throw new Error("No id_token in response");

        const response = await adLogin(idToken);
        const { data } = response.data;
        if (!data) throw new Error("AD login rejected by server");

        login(data.accessToken, data.refreshToken, data.user);
        navigate("/dashboard", { replace: true });
      })
      .catch(() => {
        navigate("/signin?error=ad_failed", { replace: true });
      });
  }, [searchParams, navigate, login]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-white dark:bg-gray-900">
      <div className="text-center">
        <div className="inline-block w-8 h-8 border-4 border-brand-500 border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-gray-500 dark:text-gray-400 text-sm">
          Completing sign in...
        </p>
      </div>
    </div>
  );
}
