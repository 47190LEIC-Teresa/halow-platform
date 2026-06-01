import { Link } from "react-router-dom";

export default function SimulationsTable({ simulations }) {
    if (simulations.length === 0) {
        return (
            <div className="empty-state-box">
                <h4>No simulations found</h4>
                <p>Run your first simulation or adjust filters.</p>
            </div>
        );
    }

    const statusOrder = {
        RUNNING: 0,
        CREATED: 1,
        FAILED: 2,
        COMPLETED: 3,
    };

    const sortedSimulations = [...simulations].sort((a, b) => {
        const statusDiff =
            (statusOrder[a.status] ?? 999) - (statusOrder[b.status] ?? 999);

        if (statusDiff !== 0) return statusDiff;

        const aEnd = a.finishedAt ? new Date(a.finishedAt).getTime() : -Infinity;
        const bEnd = b.finishedAt ? new Date(b.finishedAt).getTime() : -Infinity;

        return bEnd - aEnd;
    });

    return (
        <table className="sim-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Label</th>
                <th>Status</th>
                <th>Log Status</th>
                <th>Created</th>
                <th>Started</th>
                <th>Finished</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {sortedSimulations.map((sim) => (
                <tr key={sim.simulationId}>
                    <td>{sim.simulationId}</td>
                    <td>{sim.label || "—"}</td>
                    <td className={`status-${sim.status.toLowerCase()}`}>{sim.status}</td>
                    <td>{sim.logStatus}</td>
                    <td>{sim.createdAt}</td>
                    <td>{sim.startedAt}</td>
                    <td>{sim.finishedAt}</td>
                    <td>
                        <Link to={`/simulations/${sim.simulationId}`} className="view-link">
                            View
                        </Link>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}