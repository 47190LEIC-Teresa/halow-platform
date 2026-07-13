import { BrowserRouter, Routes, Route, Navigate, Outlet, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

import { AuthProvider, useAuth } from "./context/AuthContext";
import { AlertProvider, useAlert } from "./context/AlertContext";

import DashboardPage from "./pages/dashboard";
import LoginPage from "./pages/authentication/LoginPage";
import RegisterPage from "./pages/authentication/RegisterPage";
import ProfilePage from "./pages/ProfilePage";
import SimulationsPage from "./pages/simulations";
import SimulationDetailsPage from "./pages/simulationDetails";
import MetricsPage from "./pages/metrics";
import ProtectedLayout from "./pages/ProtectedLayout";

import "./styles/globals.css";
import "./styles/app-shell.css";
import "./styles/hero.css";
import "./styles/auth.css";
import "./styles/forms.css";
import "./styles/tables.css";
import "./styles/modal.css";
import "./styles/simulation-detail.css";
import "./styles/server-down.css";
import "./styles/metrics-page.css";
import "./styles/global-alert.css";
import "./styles/simulations-list.css";

function ProtectedRoute() {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) return <Navigate to="/" replace />;
    return <Outlet />;
}

function GlobalAlert() {
    const { alert, clearAlert } = useAlert();

    if (!alert) return null;

    return (
        <div
            className={`global-alert global-alert--${alert.type || "error"}`}
            role="alert"
        >
            <div className="global-alert__content">
                {alert.title && <strong>{alert.title}</strong>}
                <p>{alert.message}</p>
                {alert.code && (
                    <span className="global-alert__code">
                        Code: {alert.code}
                    </span>
                )}
                <button
                    type="button"
                    className="global-alert__close"
                    onClick={clearAlert}
                    aria-label="Dismiss alert"
                    title="Dismiss"
                >
                    ×
                </button>
            </div>
        </div>
    );
}

function AppContent() {
    const [serverDown, setServerDown] = useState(false);
    const navigate = useNavigate();
    const { showAlert } = useAlert();


    useEffect(() => {
        const onDown = () => setServerDown(true);

        const onAuthLost = () => {
            showAlert({
                type: "error",
                title: "Session expired",
                message: "Your session has expired. You will be redirected to login.",
                duration: 3000,
            });

            setTimeout(() => navigate("/", { replace: true }), 1500);
        };

        window.addEventListener("server-down", onDown);
        window.addEventListener("auth-lost", onAuthLost);

        return () => {
            window.removeEventListener("server-down", onDown);
            window.removeEventListener("auth-lost", onAuthLost);
        };
    }, [navigate]);

    if (serverDown) {
        return (
            <div className="server-down-page">
                <div className="server-down-card">
                    <div className="server-down-icon">⚠</div>
                    <h1>Server unavailable</h1>
                    <p>We can’t reach the backend right now. Please try again in a moment.</p>
                    <div className="server-down-actions">
                        <button onClick={() => window.location.reload()}>Retry</button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <>
            <GlobalAlert />

            <Routes>
                <Route path="/" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                <Route element={<ProtectedRoute />}>
                    <Route element={<ProtectedLayout />}>
                        <Route path="/dashboard" element={<DashboardPage />} />
                        <Route path="/profile" element={<ProfilePage />} />
                        <Route path="/metrics" element={<MetricsPage />} />

                        <Route path="/simulations">
                            <Route index element={<SimulationsPage />} />
                            <Route path=":simulationId" element={<SimulationDetailsPage />} />
                        </Route>
                    </Route>
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AlertProvider>
                    <AppContent />
                </AlertProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}