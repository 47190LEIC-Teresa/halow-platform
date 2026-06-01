import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

export function useSimulationDetails({ simulationId, authFetch, token }) {
    const [simulation, setSimulation] = useState(null);
    const [files, setFiles] = useState([]);
    const [config, setConfig] = useState(null);
    const [metrics, setMetrics] = useState(null);
    const [isMetricsOpen, setIsMetricsOpen] = useState(false);
    const [isRunningMetrics, setIsRunningMetrics] = useState(false);
    const [isRerunning, setIsRerunning] = useState(false);
    const [message, setMessage] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        let isMounted = true;

        async function loadData() {
            try {
                const simRes = await authFetch(`/api/simulations/${simulationId}`, token);
                const simData = await simRes.json();

                const filesRes = await authFetch(`/api/files/${simulationId}`, token);
                const filesData = await filesRes.json();

                const configRes = await authFetch(`/api/simulations/${simulationId}/config`, token);
                const configData = await configRes.json();

                if (!isMounted) return;

                setSimulation(simData);
                setFiles(filesData);
                setConfig(configData);

                try {
                    const metricsRes = await authFetch(`/api/simulations/${simulationId}/metrics`, token);

                    if (!metricsRes.ok) {
                        if (metricsRes.status === 204) {
                            setMetrics(null);
                            return;
                        }
                        throw new Error("Failed to load metrics");
                    }

                    const metricsData = await metricsRes.json();
                    if (isMounted) setMetrics(metricsData);
                } catch (err) {
                    if (isMounted) {
                        setMetrics(null);
                        setMessage(err.message || "Failed to load metrics");
                    }
                }
            } catch (err) {
                if (isMounted) {
                    setMessage(err.message || "Failed to load simulation");
                }
            }
        }

        loadData();

        return () => {
            isMounted = false;
        };
    }, [simulationId, authFetch, token]);

    const logFile = files.find(
        (file) => file.fileName === "log.txt" || file.fileType === "LOG"
    );

    const canRunMetrics = !!logFile && !logFile.downloaded;
    const hasMetrics = !!metrics && Object.keys(metrics).length > 0;

    const canRerun = useMemo(() => {
        if (!simulation?.status) return false;
        return simulation.status !== "CREATED" && simulation.status !== "RUNNING";
    }, [simulation]);

    async function handleDownload(file) {
        try {
            const res = await authFetch(file.downloadUrl, token);

            if (!res.ok) {
                throw new Error("Download failed");
            }

            const blob = await res.blob();
            const url = window.URL.createObjectURL(blob);

            const a = document.createElement("a");
            a.href = url;
            a.download = file.fileName;
            document.body.appendChild(a);
            a.click();
            a.remove();

            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error(err);
            setMessage(err.message || "Failed to download file");
        }
    }

    async function handleRunMetrics() {
        if (!canRunMetrics) return;

        setIsRunningMetrics(true);
        setMessage("");

        try {
            const res = await authFetch(`/api/simulations/${simulationId}/metrics`, token, {
                method: "POST",
            });

            if (!res.ok) {
                throw new Error("Failed to run metrics");
            }

            const data = await res.json();
            setMetrics(data);
            setIsMetricsOpen(false);
        } catch (err) {
            setMessage(err.message || "Failed to run metrics");
        } finally {
            setIsRunningMetrics(false);
        }
    }

    async function handleRerun() {
        if (!canRerun) return;

        setIsRerunning(true);
        setMessage("");

        try {
            const res = await authFetch(`/api/simulations/${simulationId}/rerun`, token, {
                method: "POST",
            });

            if (!res.ok) {
                throw new Error("Failed to rerun simulation");
            }

            const data = await res.json();
            setMessage(`Simulation rerun started${data?.id ? ` (#${data.id})` : ""}.`);
            navigate("/simulations");
        } catch (err) {
            setMessage(err.message || "Failed to rerun simulation");
        } finally {
            setIsRerunning(false);
        }
    }

    return {
        simulation,
        files,
        config,
        metrics,
        message,
        isMetricsOpen,
        setIsMetricsOpen,
        isRunningMetrics,
        isRerunning,
        hasMetrics,
        canRunMetrics,
        canRerun,
        handleDownload,
        handleRunMetrics,
        handleRerun,
    };
}