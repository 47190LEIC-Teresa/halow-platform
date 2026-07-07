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

export default function RegisterPage() {
    const [form, setForm] = useState({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: "",
    });
    const navigate = useNavigate();
    const { login } = useAuth();
    const { showAlert, clearAlert } = useAlert();

    const updateField = (field, value) => {
        setForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        clearAlert();

        try {
            const payload = {
                ...form,
                email: form.email.trim() || null,
            };

            const res = await fetch("/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                throw await parseApiError(res, "Failed to create user");
            }

            const data = await res.json();
            login(data.token);
            navigate("/dashboard");
        } catch (err) {
            const apiError = normalizeError(err, "Failed to create user");

            if (apiError.source === "CLIENT" && apiError.message === "Failed to fetch") {
                showServerAlert(showAlert);
                return;
            }

            showApiAlert(showAlert, apiError, "Create account failed");
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <p className="eyebrow">HaLow Platform</p>
                <h1>Create account</h1>

                <form onSubmit={handleRegister} className="auth-form">
                    <label>Username</label>
                    <input
                        value={form.username}
                        onChange={(e) => updateField("username", e.target.value)}
                        required
                    />

                    <label htmlFor="email">
                        Email <span className="muted">(optional)</span>
                    </label>
                    <p id="email-help" className="field-help">
                        Add your email to receive notifications when simulations finish and before logs expire.
                    </p>
                    <input
                        id="email"
                        type="email"
                        value={form.email}
                        onChange={(e) => updateField("email", e.target.value)}
                        aria-describedby="email-help"
                    />

                    <label>First name</label>
                    <input
                        value={form.firstName}
                        onChange={(e) => updateField("firstName", e.target.value)}
                        required
                        minLength={3}
                        title="First name must be at least 3 characters long"
                    />

                    <label>Last name</label>
                    <input
                        value={form.lastName}
                        onChange={(e) => updateField("lastName", e.target.value)}
                        required
                        minLength={3}
                        title="Last name must be at least 3 characters long"
                    />

                    <label>Password</label>
                    <input
                        type="password"
                        value={form.password}
                        onChange={(e) => updateField("password", e.target.value)}
                        required
                    />

                    <button type="submit" className="primary-btn">
                        Create account
                    </button>
                </form>

                <p className="muted">
                    Already have an account? <Link to="/">Go to login</Link>
                </p>
            </div>
        </div>
    );
}