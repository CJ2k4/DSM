import axios from "axios";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from "./tokenStore";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

const client = axios.create({ baseURL });

// Attach the access token to every request.
client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On a 401, try to refresh the access token once, then replay the original request.
// If the refresh fails, clear the session and bounce to /login.
let refreshPromise = null;

function performRefresh() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return Promise.reject(new Error("No refresh token"));

  // Use a bare axios call so we don't recurse through these interceptors.
  return axios
    .post(`${baseURL}/auth/refresh`, { refreshToken })
    .then((response) => {
      setTokens({
        accessToken: response.data.accessToken,
        refreshToken: response.data.refreshToken,
      });
      return response.data.accessToken;
    });
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    const isRefreshCall = original?.url?.includes("/auth/refresh");

    if (status === 401 && original && !original._retry && !isRefreshCall) {
      original._retry = true;
      try {
        refreshPromise = refreshPromise || performRefresh();
        const newAccessToken = await refreshPromise;
        refreshPromise = null;
        original.headers.Authorization = `Bearer ${newAccessToken}`;
        return client(original);
      } catch (refreshError) {
        refreshPromise = null;
        clearTokens();
        if (window.location.pathname !== "/login") {
          window.location.assign("/login");
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// Extracts a human-friendly message from the backend's standard error envelope.
export function extractErrorMessage(error, fallback = "Something went wrong") {
  const data = error?.response?.data;
  if (!data) return fallback;
  if (Array.isArray(data.errors) && data.errors.length > 0) {
    return data.errors.map((e) => e.message).join(", ");
  }
  return data.message || fallback;
}

export default client;
