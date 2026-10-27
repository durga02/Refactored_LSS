import axios from "axios";
import { STORAGE_KEYS } from "../lib/constants.js";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// `withCredentials: true` is required so the JSESSIONID cookie the backend
// sets for the guest shopping cart round-trips on every request, even though
// the API and the SPA run on different origins in dev (5173 vs 8080).
const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  withCredentials: true,
});

// Attach the admin JWT (if present) to every request. Public storefront
// endpoints simply ignore the header; /api/admin/** requires it.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(STORAGE_KEYS.ADMIN_TOKEN);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 on an /admin/* call means the token is missing/expired — clear it
// so ProtectedRoute bounces the user back to the login page.

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url || "";
    const isAdminRequest = /(^|\/)admin(\/|$|\?)/i.test(requestUrl);

    if (error.response?.status === 401 && isAdminRequest) {
      localStorage.removeItem(STORAGE_KEYS.ADMIN_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.ADMIN_USERNAME);

      window.dispatchEvent(new Event("admin-session-expired"));
    }

    return Promise.reject(error);
  }
);


export default api;
export { API_BASE_URL };
