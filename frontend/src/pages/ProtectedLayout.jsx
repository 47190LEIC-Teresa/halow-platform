import { NavLink, Outlet } from "react-router-dom";
import { FiLogOut } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";

export default function ProtectedLayout() {
    const { logout: authLogout } = useAuth();

    function logout() {
        authLogout();
    }

    const navClass = ({ isActive }) =>
        isActive ? "nav-item active" : "nav-item";

    return (
        <div className="app-shell">

            <aside className="sidebar">
                <div className="brand">
                    <div className="brand-badge">H</div>
                    <div>
                        <h1>HaLow Platform</h1>
                        <p>Simulation workspace</p>
                    </div>
                </div>

                <nav className="nav">
                    <NavLink className={navClass} to="/dashboard">
                        Dashboard
                    </NavLink>
                    <NavLink className={navClass} to="/simulations">
                        Simulations
                    </NavLink>
                    <NavLink className={navClass} to="/metrics">
                        Metrics
                    </NavLink>
                </nav>

                <div className="sidebar-bottom">
                    <NavLink className={navClass} to="/profile">
                        Profile
                    </NavLink>
                </div>
            </aside>

            <div className="content-shell">
                <header className="utility-bar">
                    <div className="utility-actions">
                        <button
                            type="button"
                            onClick={logout}
                            className="utility-btn"
                            title="Logout"
                        >
                            <FiLogOut />
                        </button>
                    </div>
                </header>

                <main className="main-content">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}