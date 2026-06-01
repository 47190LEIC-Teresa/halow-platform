import { createContext, useContext, useState } from "react";

const AuthContext = createContext();

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem("token"));

    const login = (newToken) => {
        localStorage.setItem("token", newToken);
        setToken(newToken);
    };

    const logout = () => {
        localStorage.removeItem("token");
        setToken(null);
    };


    async function authFetch(url, token, options = {}) {
        try {
            const isFormData = options.body instanceof FormData;

            const res = await fetch(url, {
                ...options,
                credentials: "include",
                headers: {
                    Authorization: `Bearer ${token}`,
                    ...(isFormData ? {} : { "Content-Type": "application/json" }),
                    ...options.headers,
                },
            });

            if (res.status === 401 || res.status === 403) {
                window.dispatchEvent(new CustomEvent("auth-lost"));
                throw new Error("Unauthorized");
            }

            if (!res.ok) {
                const text = await res.text();
                console.error(`AuthFetch failed for ${url}:`, res.status, text);
                throw new Error(text || `HTTP ${res.status}`);
            }

            return res;
        } catch (err) {
            if (err.message === "Failed to fetch") {
                window.dispatchEvent(new CustomEvent("server-down"));
            }
            throw err;
        }
    }
    const isAuthenticated = !!token;

    return (
        <AuthContext.Provider value={{ token, login, logout, isAuthenticated, authFetch }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}
