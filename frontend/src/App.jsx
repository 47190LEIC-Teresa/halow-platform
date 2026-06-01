import { BrowserRouter, Routes, Route, Navigate, Outlet } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/authentication/LoginPage";
import RegisterPage from "./pages/authentication/RegisterPage";
import ProfilePage from "./pages/ProfilePage";
import SimulationsPage from "./pages/simulations";
import SimulationDetailsPage from "./pages/simulationDetails";
import MetricsPage from "./pages/metrics";
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
import { useEffect, useState } from "react";
import ProtectedLayout from "./pages/ProtectedLayout";

function ProtectedRoute() {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) return <Navigate to="/" replace />;
    return <Outlet />;
}

function AppContent() {
    const [serverDown, setServerDown] = useState(false);

    useEffect(() => {
        const onDown = () => setServerDown(true);
        window.addEventListener("server-down", onDown);
        return () => window.removeEventListener("server-down", onDown);
    }, []);

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
        <AuthProvider>
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

                {/* Catch-all — unknown routes redirect to login */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </AuthProvider>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <AppContent />
        </BrowserRouter>
    );
}