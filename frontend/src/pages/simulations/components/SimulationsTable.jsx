import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";

function IndeterminateCheckbox({
                                   checked,
                                   indeterminate,
                                   onChange,
                                   ariaLabel,
                                   className = "",
                               }) {
    const ref = useRef(null);

    useEffect(() => {
        if (ref.current) {
            ref.current.indeterminate = indeterminate;
        }
    }, [indeterminate]);

    return (
        <input
            ref={ref}
            type="checkbox"
            checked={checked}
            onChange={onChange}
            aria-label={ariaLabel}
            className={className}
        />
    );
}

export default function SimulationsTable({
                                             simulations = [],
                                             viewMode,
                                             isSelectionMode,
                                             selectedIds,
                                             onToggleSimulation,
                                             onToggleMany,
                                             onSelectAll,
                                         }) {
    const [openGroups, setOpenGroups] = useState({});
    const [singleRunsOpen, setSingleRunsOpen] = useState(false);

    const statusOrder = {
        RUNNING: 0,
        CREATED: 1,
        FAILED: 2,
        COMPLETED: 3,
    };

    const sortedSimulations = useMemo(() => {
        return [...simulations].sort((a, b) => {
            const statusDiff =
                (statusOrder[a.status] ?? 999) - (statusOrder[b.status] ?? 999);

            if (statusDiff !== 0) return statusDiff;

            const aEnd = a.finishedAt ? new Date(a.finishedAt).getTime() : -Infinity;
            const bEnd = b.finishedAt ? new Date(b.finishedAt).getTime() : -Infinity;

            return bEnd - aEnd;
        });
    }, [simulations]);

    const unlabeledSimulations = useMemo(
        () => sortedSimulations.filter((sim) => !sim.label?.trim()),
        [sortedSimulations]
    );

    const groupedSimulations = useMemo(() => {
        return sortedSimulations.reduce((acc, sim) => {
            const key = sim.label?.trim();
            if (!key) return acc;
            if (!acc[key]) acc[key] = [];
            acc[key].push(sim);
            return acc;
        }, {});
    }, [sortedSimulations]);

    const groupEntries = useMemo(
        () => Object.entries(groupedSimulations),
        [groupedSimulations]
    );

    const visibleIds = useMemo(
        () => sortedSimulations.map((sim) => sim.simulationId),
        [sortedSimulations]
    );

    const allVisibleSelected =
        visibleIds.length > 0 &&
        visibleIds.every((id) => selectedIds.has(id));

    const someVisibleSelected =
        visibleIds.some((id) => selectedIds.has(id)) && !allVisibleSelected;

    function toggleGroup(label) {
        setOpenGroups((prev) => ({
            ...prev,
            [label]: !prev[label],
        }));
    }

    function getStatusSummary(items) {
        const counts = items.reduce((acc, sim) => {
            acc[sim.status] = (acc[sim.status] || 0) + 1;
            return acc;
        }, {});

        return Object.entries(counts)
            .map(([status, count]) => `${status}: ${count}`)
            .join(" • ");
    }

    function isFullySelected(items) {
        return items.length > 0 && items.every((sim) => selectedIds.has(sim.simulationId));
    }

    function isPartiallySelected(items) {
        const selectedCount = items.filter((sim) =>
            selectedIds.has(sim.simulationId)
        ).length;

        return selectedCount > 0 && selectedCount < items.length;
    }

    function handleHeaderSelectAll() {
        onSelectAll(visibleIds);
    }

    function handleGroupToggleMany(items) {
        const ids = items.map((sim) => sim.simulationId);
        onToggleMany(ids);
    }

    function renderRow(sim, showLabel, selectable) {
        const isSelected = selectedIds.has(sim.simulationId);

        return (
            <tr key={sim.simulationId} className={isSelected ? "is-selected" : ""}>
                {selectable && isSelectionMode && (
                    <td className="selection-cell">
                        <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => onToggleSimulation(sim.simulationId)}
                            aria-label={`Select simulation ${sim.simulationId}`}
                        />
                    </td>
                )}
                <td>{sim.simulationId}</td>
                {showLabel && <td>{sim.label || "—"}</td>}
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
        );
    }

    if (simulations.length === 0) {
        return (
            <div className="empty-state-box">
                <h4>No simulations found</h4>
                <p>Run your first simulation or adjust filters.</p>
            </div>
        );
    }

    return (
        <div className="simulation-table-wrap">
            {viewMode === "all" ? (
                <table className="sim-table">
                    <thead>
                    <tr>
                        {isSelectionMode && (
                            <th className="selection-cell">
                                <IndeterminateCheckbox
                                    checked={allVisibleSelected}
                                    indeterminate={someVisibleSelected}
                                    onChange={handleHeaderSelectAll}
                                    ariaLabel="Select all simulations"
                                />
                            </th>
                        )}
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
                    {sortedSimulations.map((sim) => renderRow(sim, true, true))}
                    </tbody>
                </table>
            ) : (
                <div className="simulation-groups">
                    {unlabeledSimulations.length > 0 && (
                        <div
                            className={`simulation-group-card ${
                                isFullySelected(unlabeledSimulations) ? "group-selected" : ""
                            }`}
                        >
                            <button
                                type="button"
                                className="simulation-group-header"
                                onClick={() => setSingleRunsOpen((prev) => !prev)}
                            >
                                <div className="simulation-group-header-left">
                                    {isSelectionMode && (
                                        <span
                                            className="group-checkbox-wrap"
                                            onClick={(e) => e.stopPropagation()}
                                        >
                                            <IndeterminateCheckbox
                                                checked={isFullySelected(unlabeledSimulations)}
                                                indeterminate={isPartiallySelected(unlabeledSimulations)}
                                                onChange={() =>
                                                    handleGroupToggleMany(unlabeledSimulations)
                                                }
                                                ariaLabel="Select single runs group"
                                            />
                                        </span>
                                    )}
                                    <span className="simulation-group-label">Single runs</span>
                                    <span className="simulation-group-count">
                                        {unlabeledSimulations.length} run
                                        {unlabeledSimulations.length !== 1 ? "s" : ""}
                                    </span>
                                </div>

                                <div className="simulation-group-header-right">
                                    <span className="simulation-group-chevron">
                                        {singleRunsOpen ? "▾" : "▸"}
                                    </span>
                                </div>
                            </button>

                            {singleRunsOpen && (
                                <div className="simulation-group-body">
                                    <table className="sim-table">
                                        <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Status</th>
                                            <th>Log Status</th>
                                            <th>Created</th>
                                            <th>Started</th>
                                            <th>Finished</th>
                                            <th>Actions</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {unlabeledSimulations.map((sim) =>
                                            renderRow(sim, false, false)
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}

                    {groupEntries.map(([label, items]) => {
                        const isOpen = !!openGroups[label];
                        const isSelected = isFullySelected(items);
                        const isPartial = isPartiallySelected(items);

                        return (
                            <div
                                key={label}
                                className={`simulation-group-card ${
                                    isSelected ? "group-selected" : ""
                                }`}
                            >
                                <button
                                    type="button"
                                    className="simulation-group-header"
                                    onClick={() => toggleGroup(label)}
                                >
                                    <div className="simulation-group-header-left">
                                        {isSelectionMode && (
                                            <span
                                                className="group-checkbox-wrap"
                                                onClick={(e) => e.stopPropagation()}
                                            >
                                                <IndeterminateCheckbox
                                                    checked={isSelected}
                                                    indeterminate={isPartial}
                                                    onChange={() => handleGroupToggleMany(items)}
                                                    ariaLabel={`Select group ${label}`}
                                                />
                                            </span>
                                        )}
                                        <span className="simulation-group-label">{label}</span>
                                        <span className="simulation-group-count">
                                            {items.length} run{items.length !== 1 ? "s" : ""}
                                        </span>
                                    </div>

                                    <div className="simulation-group-header-right">
                                        <span className="simulation-group-summary">
                                            {getStatusSummary(items)}
                                        </span>
                                        <span className="simulation-group-chevron">
                                            {isOpen ? "▾" : "▸"}
                                        </span>
                                    </div>
                                </button>

                                {isOpen && (
                                    <div className="simulation-group-body">
                                        <table className="sim-table">
                                            <thead>
                                            <tr>
                                                <th>ID</th>
                                                <th>Status</th>
                                                <th>Log Status</th>
                                                <th>Created</th>
                                                <th>Started</th>
                                                <th>Finished</th>
                                                <th>Actions</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {items.map((sim) => renderRow(sim, false, false))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}