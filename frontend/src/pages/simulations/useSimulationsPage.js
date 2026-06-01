import { useEffect, useState } from "react";
import { simulationFormDefaults } from "./simulationFormDefaults";
import { buildSimulationPayload, buildSimulationBatchPayload } from "./simulationPayload";

export function useSimulationsPage(authFetch, token) {
    const [simulations, setSimulations] = useState([]);
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [search, setSearch] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [showOptional, setShowOptional] = useState(false);
    const [message, setMessage] = useState("");
    const [result, setResult] = useState(null);
    const [form, setForm] = useState(simulationFormDefaults);

    async function fetchSimulations() {
        try {
            const res = await authFetch("/api/simulations", token);
            const data = await res.json();
            console.log("Fetched simulations:", data);
            setSimulations(data);
        } catch (err) {
            console.error("Failed to fetch simulations:", err);
        }
    }

    useEffect(() => {
        void fetchSimulations(); // already handles its own try/catch
    }, [authFetch, token]);

    const hasActiveSimulations = simulations.some(
        (sim) =>
            sim.status === "PENDING" ||
            sim.status === "RUNNING" ||
            sim.status === "CREATED"
    );

    useEffect(() => {
        if (!hasActiveSimulations) return;

        const intervalId = setInterval(() => {
            void fetchSimulations();
        }, 3000);

        return () => clearInterval(intervalId);
    }, [hasActiveSimulations, authFetch, token, simulations]);

    const filteredSimulations = simulations.filter((sim) => {
        const matchesStatus = statusFilter === "ALL" || sim.status === statusFilter;
        const matchesSearch =
            sim.simulationId.toString().includes(search) ||
            (sim.label && sim.label.toLowerCase().includes(search.toLowerCase()));

        return matchesStatus && matchesSearch;
    });

    function updateField(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    async function startSimulation(e) {
        e.preventDefault();
        setMessage("Starting simulation...");
        setResult(null);

        try {
            const isBatch = form.runMode === "batch";
            const payload = isBatch
                ? buildSimulationBatchPayload(form)
                : buildSimulationPayload(form);

            console.log("Starting simulation with payload:", payload);

            const endpoint = isBatch
                ? "/api/simulations/batch"
                : "/api/simulations";

            const formData = new FormData();

            formData.append(
                "request",
                new Blob([JSON.stringify(payload)], { type: "application/json" })
            );

            if (form.fileGroups) {
                formData.append("fileGroups", form.fileGroups);
            }

            const res = await authFetch(endpoint, token, {
                method: "POST",
                body: formData,
            });

            const text = await res.text();
            const data = text ? JSON.parse(text) : null;

            if (!res.ok) {
                throw new Error(data.message || "Failed to start simulation");
            }

            if (isBatch) {
                setSimulations((prev) => [...(data || []), ...prev]);
                setMessage("Batch started successfully.");
            } else {
                setResult(data);
                setSimulations((prev) => [data, ...prev]);
                setMessage("Simulation started successfully.");
            }

            setShowForm(false);
        } catch (err) {
            setMessage(err.message || "Failed to start simulation");
        }
    }

    return {
        simulations,
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
        result,
        form,
        setForm,
        updateField,
        startSimulation,
    };
}