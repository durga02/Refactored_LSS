
import { createContext, useContext, useEffect, useState } from "react";
import * as adminApi from "../api/admin";
import { STORAGE_KEYS } from "../lib/constants.js";

const AuthContext = createContext(null);
const ADMIN_SESSION_EXPIRED_EVENT = "admin-session-expired";

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() =>
    localStorage.getItem(STORAGE_KEYS.ADMIN_TOKEN)
  );
  const [username, setUsername] = useState(() =>
    localStorage.getItem(STORAGE_KEYS.ADMIN_USERNAME)
  );

  const logout = () => {
    localStorage.removeItem(STORAGE_KEYS.ADMIN_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.ADMIN_USERNAME);
    setToken(null);
    setUsername(null);
  };

  useEffect(() => {
    const handleSessionExpired = () => logout();

    window.addEventListener(
      ADMIN_SESSION_EXPIRED_EVENT,
      handleSessionExpired
    );

    return () => {
      window.removeEventListener(
        ADMIN_SESSION_EXPIRED_EVENT,
        handleSessionExpired
      );
    };
  }, []);

  const login = async (user, password) => {
    const data = await adminApi.adminLogin(user, password);
    localStorage.setItem(STORAGE_KEYS.ADMIN_TOKEN, data.token);
    localStorage.setItem(STORAGE_KEYS.ADMIN_USERNAME, data.username);
    setToken(data.token);
    setUsername(data.username);
    return data;
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        username,
        isAuthenticated: Boolean(token),
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}

export { ADMIN_SESSION_EXPIRED_EVENT };
