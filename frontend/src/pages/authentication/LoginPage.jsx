import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useAlert } from "../../context/AlertContext";
import {
    parseApiError,
    normalizeError,
    showApiAlert,
    showServerAlert,
} from "../../utils/apiAlerts";

export default function LoginPage() {
    const [identifier, setIdentifier] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();
    const { login } = useAuth();
    const { showAlert, clearAlert } = useAlert();

    const handleLogin = async (e) => {
        e.preventDefault();
        clearAlert();

        try {
            const res = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ identifier, password }),
            });

            if (!res.ok) {
                throw await parseApiError(res, "Login failed");
            }

            const data = await res.json();
            login(data.token);
            navigate("/dashboard");
        } catch (err) {
            const apiError = normalizeError(err, "Login failed");

            if (apiError.source === "CLIENT" && apiError.message === "Failed to fetch") {
                showServerAlert(showAlert);
                return;
            }

            showApiAlert(showAlert, apiError, "Login failed");
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <p className="eyebrow">HaLow Platform</p>
                <h1>Login</h1>

                <form onSubmit={handleLogin} className="auth-form">
                    <label>Username or Email</label>
                    <input
                        value={identifier}
                        onChange={(e) => setIdentifier(e.target.value)}
                        required
                    />

                    <label>Password</label>
                    <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />

                    <button type="submit" className="primary-btn">
                        Enter
                    </button>
                </form>

                <p className="muted">
                    No account yet? <Link to="/register">Create one</Link>
                </p>
            </div>
        </div>
    );
}