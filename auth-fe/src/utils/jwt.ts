interface JwtPayload {
  exp?: number;
  groups?: string[];
}

function decodePayload(token: string): JwtPayload {
  try {
    const [, part] = token.split(".");
    const padded = part.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(padded)) as JwtPayload;
  } catch {
    return {};
  }
}

/** Returns true if the token is expired or cannot be decoded. */
export function isTokenExpired(token: string): boolean {
  const { exp } = decodePayload(token);
  if (!exp) return true;
  return Date.now() / 1000 >= exp;
}

/** Extracts the `groups` claim from the JWT payload without library overhead. */
export function decodeJwtGroups(token: string): string[] {
  return decodePayload(token).groups ?? [];
}
