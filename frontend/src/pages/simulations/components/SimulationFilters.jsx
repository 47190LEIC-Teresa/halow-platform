export default function SimulationFilters({
                                              search,
                                              onSearchChange,
                                              statusFilter,
                                              onStatusChange,
                                              viewMode,
                                              onViewModeChange,
                                              isSelectionMode,
                                              onEnterSelectionMode,
                                              onDownloadFiles,
                                              onCancelSelection,
                                              hasSelection,
                                          }) {
    return (
        <div className="simulation-toolbar">
            <div className="simulation-controls-row">
                <div
                    className="simulation-view-toggle"
                    role="group"
                    aria-label="View mode"
                >
                    <span
                        className={`view-toggle-slider ${
                            viewMode === "all" ? "slide-right" : "slide-left"
                        }`}
                        aria-hidden="true"
                    />
                    <button
                        type="button"
                        className={viewMode === "grouped" ? "toggle-btn active" : "toggle-btn"}
                        onClick={() => onViewModeChange("grouped")}
                    >
                        Grouped
                    </button>
                    <button
                        type="button"
                        className={viewMode === "all" ? "toggle-btn active" : "toggle-btn"}
                        onClick={() => onViewModeChange("all")}
                    >
                        All
                    </button>
                </div>

                <div
                    className="simulation-selection-actions"
                    style={{display: "flex", gap: "12px"}}
                >
                    {!isSelectionMode ? (
                        <button
                            type="button"
                            className="secondary-btn"
                            onClick={onEnterSelectionMode}
                        >
                            Select simulations
                        </button>
                    ) : (
                        <>
                            <button
                                type="button"
                                className="secondary-btn"
                                onClick={onDownloadFiles}
                                disabled={!hasSelection}
                            >
                                Download files
                            </button>

                            <button
                                type="button"
                                className="secondary-btn"
                                onClick={onCancelSelection}
                            >
                                Cancel
                            </button>
                        </>
                    )}
                </div>
            </div>

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
                    <option value="FAILED">Failed</option>
                    <option value="RUNNING">Running</option>
                    <option value="CREATED">Created</option>
                </select>
            </div>
        </div>
    );
}