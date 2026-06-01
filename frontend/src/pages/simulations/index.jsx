import { FiPlus } from "react-icons/fi";
import SimulationFilters from "./components/SimulationFilters";
import SimulationsTable from "./components/SimulationsTable";
import NewSimulationModal from "./components/NewSimulationModal";
import { useAuth } from "../../context/AuthContext";
import { useSimulationsPage } from "./useSimulationsPage";
import {simulationFormDefaults} from "./simulationFormDefaults";


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
                />

                <SimulationsTable simulations={filteredSimulations} />
            </section>

            {showForm && (
                <NewSimulationModal
                    form={form}
                    showOptional={showOptional}
                    setShowOptional={setShowOptional}
                    updateField={updateField}
                    onClose={() => {
                        setShowForm(false)
                        setForm(simulationFormDefaults)
                    }}
                    onSubmit={startSimulation}
                />
            )}
        </>
    );
}