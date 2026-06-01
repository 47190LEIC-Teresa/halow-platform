export default function SimulationFilters({
                                              search,
                                              onSearchChange,
                                              statusFilter,
                                              onStatusChange,
                                          }) {
    return (
        <div className="filter-bar">
            <input
                className="filter-input"
                placeholder="Search simulations..."
                value={search}
                onChange={(e) => onSearchChange(e.target.value)}
            />
            <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => onStatusChange(e.target.value)}
            >
                <option value="ALL">All Statuses</option>
                <option value="COMPLETED">Completed</option>
                <option value="RUNNING">Running</option>
                <option value="QUEUED">Queued</option>
                <option value="FAILED">Failed</option>
            </select>
        </div>
    );
}