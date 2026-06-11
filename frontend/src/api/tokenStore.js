// Small localStorage-backed token store. Kept separate from the auth context so the
// axios interceptor can read/write tokens without importing React state (avoids cycles).

const ACCESS_KEY = "dsm.accessToken";
const REFRESH_KEY = "dsm.refreshToken";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY);
}

export function setTokens({ accessToken, refreshToken }) {
  if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export function hasTokens() {
  return Boolean(getAccessToken() && getRefreshToken());
}
