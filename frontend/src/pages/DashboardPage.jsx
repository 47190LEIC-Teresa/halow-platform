import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function DashboardPage() {
    const [user, setUser] = useState(null);
    const [stats, setStats] = useState({ total: 0, running: 0, completed: 0 });
    const [recentSims, setRecentSims] = useState([]);

    const { authFetch, token } = useAuth();

    useEffect(() => {
        authFetch("/api/user", token)
            .then((res) => {
                if (!res.ok) throw new Error("Failed to load user");
                return res.json();
            })
            .then(setUser)
            .catch(() => setUser(null));
    }, [authFetch, token]);

    useEffect(() => {
        authFetch("/api/simulations", token)
            .then((res) => res.json())
            .then((sims) => {
                setStats({
                    total: sims.length,
                    running: sims.filter((s) => s.status === "RUNNING").length,
                    completed: sims.filter((s) => s.status === "COMPLETED").length,
                });
                setRecentSims(sims.slice(-5).reverse());
            })
            .catch(() => {});
    }, [authFetch, token]);

    return (
        <>

            <div>
                <p className="eyebrow">Overview</p>
                <h2>Welcome{user ? `, ${user.firstName}` : ""}</h2>
                <p>
                    Configure, launch, and monitor IEEE 802.11ah simulations.
                    Track your runs, review results, and manage your account from here.
                </p>
            </div>

            <section className="stats-grid">
                <article className="stat-card">
                    <strong>{stats.running}</strong>
                    <span>Running</span>
                </article>

                <article className="stat-card">
                    <strong>{stats.completed}</strong>
                    <span>Completed</span>
                </article>

                <article className="stat-card">
                    <strong>{stats.total}</strong>
                    <span>Total Simulations</span>
                </article>
            </section>

            <section className="hero" style={{ marginBottom: "24px" }}>
                <div className="panel-header">
                    <div>
                        <p className="eyebrow">History</p>
                        <h3>Recent Simulations</h3>
                    </div>
                </div>

                {recentSims.length > 0 ? (
                    <table className="sim-table">
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Status</th>
                            <th>Created</th>
                        </tr>
                        </thead>
                        <tbody>
                        {recentSims.map((sim) => (
                            <tr key={sim.simulationId}>
                                <td>{sim.simulationId}</td>
                                <td className={`status-${sim.status.toLowerCase()}`}>
                                    {sim.status}
                                </td>
                                <td>{sim.createdAt}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                ) : (
                    <div className="empty-state-box">
                        <h4>No simulations yet</h4>
                        <p>Run your first simulation to see results here.</p>
                        <Link to="/simulations" className="primary-btn">
                            New Simulation
                        </Link>
                    </div>
                )}
            </section>
        </>
    );
}