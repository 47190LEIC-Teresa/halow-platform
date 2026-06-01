import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function ProfilePage() {
    const { token, authFetch } = useAuth();

    const [username, setUsername] = useState("");
    const [form, setForm] = useState({
        email: "",
        firstName: "",
        lastName: "",
        password: "",
    });
    const [initialForm, setInitialForm] = useState({
        email: "",
        firstName: "",
        lastName: "",
        password: "",
    });
    const [message, setMessage] = useState("");
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        authFetch("/api/user", token)
            .then((res) => {
                if (!res.ok) throw new Error("Failed to load profile");
                return res.json();
            })
            .then((user) => {
                const loadedForm = {
                    email: user.email || "",
                    firstName: user.firstName || "",
                    lastName: user.lastName || "",
                    password: "",
                };

                setUsername(user.username || "");
                setForm(loadedForm);
                setInitialForm(loadedForm);
            })
            .catch((err) => setMessage(err.message));
    }, [authFetch, token]);

    function updateField(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    function handleEdit() {
        setMessage("");
        setIsEditing(true);
    }

    function handleCancel() {
        setForm({ ...initialForm, password: "" });
        setMessage("");
        setIsEditing(false);
    }

    async function handleSave(e) {
        e.preventDefault();
        setMessage("Saving changes...");

        try {
            const payload = {
                email: form.email,
                firstName: form.firstName,
                lastName: form.lastName,
                password: form.password || null,
            };

            const res = await authFetch("/api/user", token, {
                method: "PATCH",
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || "Failed to update profile");
            }

            const updatedForm = {
                email: form.email,
                firstName: form.firstName,
                lastName: form.lastName,
                password: "",
            };

            setInitialForm(updatedForm);
            setForm(updatedForm);
            setMessage("Profile updated successfully.");
            setIsEditing(false);
        } catch (err) {
            setMessage(err.message);
        }
    }

    return (
        <>
            <div className="panel-header-with-action">
                <div>
                    <p className="eyebrow">Account</p>
                    <h2>Profile settings</h2>
                </div>

                {!isEditing ? (
                    <button type="button" className="secondary-btn" onClick={handleEdit}>
                        Edit
                    </button>
                ) : (
                    <div className="topbar-actions">
                        <button
                            type="button"
                            className="secondary-btn"
                            onClick={handleCancel}
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            form="profile-form"
                            className="primary-btn"
                        >
                            Save changes
                        </button>
                    </div>
                )}
            </div>
            <section className="panel">


                {message && <p className="message">{message}</p>}

                {!isEditing ? (
                    <div className="config-list">
                        <div className="config-row">
                            <span className="config-label">Username</span>
                            <span className="config-value">{username || "—"}</span>
                        </div>
                        <div className="config-row">
                            <span className="config-label">Email</span>
                            <span className="config-value">{form.email || "—"}</span>
                        </div>
                        <div className="config-row">
                            <span className="config-label">First name</span>
                            <span className="config-value">{form.firstName || "—"}</span>
                        </div>
                        <div className="config-row">
                            <span className="config-label">Last name</span>
                            <span className="config-value">{form.lastName || "—"}</span>
                        </div>
                    </div>
                ) : (
                    <form id="profile-form" onSubmit={handleSave} className="simulation-form">
                        <div className="config-list">
                            <div className="config-row">
                                <span className="config-label">Username</span>
                                <span className="config-value">{username || "—"}</span>
                            </div>
                        </div>

                        <div className="form-grid" style={{marginTop: "16px"}}>
                            <div className="form-row">
                                <label>Email</label>
                                <input
                                    type="email"
                                    value={form.email}
                                    onChange={(e) => updateField("email", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>First name</label>
                                <input
                                    value={form.firstName}
                                    onChange={(e) => updateField("firstName", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>Last name</label>
                                <input
                                    value={form.lastName}
                                    onChange={(e) => updateField("lastName", e.target.value)}
                                />
                            </div>

                            <div className="form-row">
                                <label>New password</label>
                                <input
                                    type="password"
                                    value={form.password}
                                    onChange={(e) => updateField("password", e.target.value)}
                                    placeholder="Leave blank to keep current password"
                                />
                            </div>
                        </div>
                    </form>
                )}
            </section>
        </>
    );
}