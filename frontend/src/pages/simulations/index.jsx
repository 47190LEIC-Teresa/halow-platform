import { useState } from "react";
import { FiPlus } from "react-icons/fi";
import SimulationFilters from "./components/SimulationFilters";
import SimulationsTable from "./components/SimulationsTable";
import NewSimulationModal from "./components/NewSimulationModal";
import { useAuth } from "../../context/AuthContext";
import { useSimulationsPage } from "./useSimulationsPage";
import { simulationFormDefaults } from "./simulationFormDefaults";

export default function SimulationsPage() {
    const { authFetch, token } = useAuth();

    const {
        filteredSimulations,
        statusFilter,
        setStatusFilter,
        search,
        setSearch,
        showForm,
        setShowForm,
        showOptional,
        setShowOptional,
        message,
        form,
        setForm,
        updateField,
        startSimulation,
    } = useSimulationsPage(authFetch, token);

    // Shared table state (view + selection) lives here
    const [viewMode, setViewMode] = useState("grouped"); // "grouped" | "all"
    const [isSelectionMode, setIsSelectionMode] = useState(false);
    const [selectedIds, setSelectedIds] = useState(new Set());

    const hasSelection = selectedIds.size > 0;
    const allSelected = false; // table will compute this and call onSelectAll appropriately

    function handleEnterSelectionMode() {
        setIsSelectionMode(true);
    }

    function handleCancelSelection() {
        setSelectedIds(new Set());
        setIsSelectionMode(false);
    }

    function handleSelectAll(requestedIds) {
        // requestedIds is an array of IDs from the table (visible IDs)
        const allSelectedNow = requestedIds.length > 0 && requestedIds.every(id => selectedIds.has(id));
        const next = new Set(selectedIds);

        if (allSelectedNow) {
            requestedIds.forEach(id => next.delete(id));
        } else {
            requestedIds.forEach(id => next.add(id));
        }

        setSelectedIds(next);
    }

    function handleToggleSimulation(id) {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    }

    function handleToggleMany(ids) {
        const allSelectedNow = ids.length > 0 && ids.every(id => selectedIds.has(id));
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (allSelectedNow) {
                ids.forEach(id => next.delete(id));
            } else {
                ids.forEach(id => next.add(id));
            }
            return next;
        });
    }

    async function handleDownloadFiles() {
        const ids = Array.from(selectedIds);

        if (!ids.length) return;

        try {
            const response = await authFetch("/api/files/download-selected", token, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    simulationIds: ids,
                }),
            });

            const blob = await response.blob();

            const disposition = response.headers.get("Content-Disposition") || "";
            const fileNameMatch =
                disposition.match(/filename\*=UTF-8''([^;]+)/) ||
                disposition.match(/filename="([^"]+)"/) ||
                disposition.match(/filename=([^;]+)/);

            const fileName = fileNameMatch
                ? decodeURIComponent(fileNameMatch[1].replace(/["']/g, "").trim())
                : "selected-simulations-files.zip";

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");

            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();

            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error("Bulk download failed:", error);
        }
    }

    return (
        <>
            <div className="panel-header-with-action">
                <div>
                    <p className="eyebrow">Simulations</p>
                    <h2>Manage and review simulation runs</h2>
                </div>

                <button className="primary-btn" onClick={() => setShowForm(true)}>
                    <FiPlus />
                </button>
            </div>

            <section className="panel">
                <div className="panel-header">
                    <div>
                        <h3>Simulation records</h3>
                        <p className="helper-text">
                            Review completed, failed, and older simulation runs.
                        </p>
                    </div>
                </div>

                <SimulationFilters
                    search={search}
                    onSearchChange={setSearch}
                    statusFilter={statusFilter}
                    onStatusChange={setStatusFilter}
                    viewMode={viewMode}
                    onViewModeChange={setViewMode}
                    isSelectionMode={isSelectionMode}
                    onEnterSelectionMode={handleEnterSelectionMode}
                    onSelectAll={handleSelectAll}
                    onDownloadFiles={handleDownloadFiles}
                    onCancelSelection={handleCancelSelection}
                    allSelected={allSelected}      // toolbar label only; table drives actual logic
                    hasSelection={hasSelection}
                />

                <SimulationsTable
                    simulations={filteredSimulations}
                    viewMode={viewMode}
                    isSelectionMode={isSelectionMode}
                    selectedIds={selectedIds}
                    onToggleSimulation={handleToggleSimulation}
                    onToggleMany={handleToggleMany}
                    onSelectAll={handleSelectAll}
                />
            </section>

            {showForm && (
                <NewSimulationModal
                    form={form}
                    showOptional={showOptional}
                    setShowOptional={setShowOptional}
                    updateField={updateField}
                    onClose={() => {
                        setShowForm(false);
                        setForm(simulationFormDefaults);
                    }}
                    onSubmit={startSimulation}
                />
            )}
        </>
    );
}