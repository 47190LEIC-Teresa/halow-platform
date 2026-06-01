import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function RegisterPage() {
    const [form, setForm] = useState({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: "",
    });
    const [message, setMessage] = useState("");
    const navigate = useNavigate();
    const { login } = useAuth();

    const updateField = (field, value) => {
        setForm((prev) => ({ ...prev, [field]: value }));
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setMessage("Creating account...");

        try {
            const res = await fetch("/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(form),
            });

            if (!res.ok) {
                const data = await res.json();
                throw new Error(data.message || "Failed to create user");
            }

            const data = await res.json();
            login(data.token);
            navigate("/dashboard");
        } catch (err) {
            setMessage(err.message);
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

                    <label>Email</label>
                    <input
                        type="email"
                        value={form.email}
                        onChange={(e) => updateField("email", e.target.value)}
                        required
                    />

                    <label>First name</label>
                    <input
                        value={form.firstName}
                        onChange={(e) => updateField("firstName", e.target.value)}
                        required
                    />

                    <label>Last name</label>
                    <input
                        value={form.lastName}
                        onChange={(e) => updateField("lastName", e.target.value)}
                        required
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

                {message && <p className="message">{message}</p>}

                <p className="muted">
                    Already have an account? <Link to="/">Go to login</Link>
                </p>
            </div>
        </div>
    );
}
