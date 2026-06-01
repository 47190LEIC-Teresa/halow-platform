import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAlert } from "../../context/AlertContext";
import {
    parseApiError,
    normalizeError,
    showApiAlert,
    showServerAlert,
} from "../../utils/apiAlerts";

export function useSimulationDetails({ simulationId, authFetch, token }) {
    const [simulation, setSimulation] = useState(null);
    const [files, setFiles] = useState([]);
    const [config, setConfig] = useState(null);
    const [metrics, setMetrics] = useState(null);

    const [isMetricsOpen, setIsMetricsOpen] = useState(false);
    const [isRunningMetrics, setIsRunningMetrics] = useState(false);
    const [isRerunning, setIsRerunning] = useState(false);

    const [message, setMessage] = useState("");
    const [simulationFailure, setSimulationFailure] = useState(null);
    const [metricsFailure, setMetricsFailure] = useState(null);

    const navigate = useNavigate();
    const { showAlert } = useAlert();

    function extractSimulationFailure(simData) {
        if (!simData?.errorMsg) return null;

        return {
            title: "Simulation failed",
            message: simData.errorMsg,
        };
    }

    function extractMetricsFailure(simData) {
        if (!simData?.metricsErrorMsg) return null;

        return {
            title: "Metrics warning",
            message: simData.metricsErrorMsg,
            code: null,
            isWarning: true,
        };
    }

    function handleClientOrApiAlert(error, fallbackTitle) {
        if (error.source === "CLIENT" && !error.status) {
            showServerAlert(showAlert);
        } else {
            showApiAlert(showAlert, error, fallbackTitle);
        }
    }

    async function fetchMetricsIfExpected(simData, initialMetricsFailure) {
        const simulationCompleted = simData?.status === "COMPLETED";
        const metricsNeverRequested = simData?.metricsStatus === "NOT_REQUESTED";

        if (!simulationCompleted || metricsNeverRequested) {
            setMetrics(null);
            return;
        }

        try {
            const response = await authFetch(`/api/simulations/${simulationId}/metrics`, token);

            if (response.status === 204) {
                setMetrics(null);
                return;
            }

            const metricsData = await response.json();
            setMetrics(metricsData);
        } catch (err) {
            setMetrics(null);

            const metricsLoadError = normalizeError(err, "Failed to load metrics");

            if (!initialMetricsFailure) {
                setMetricsFailure({
                    title: "Metrics warning",
                    message: metricsLoadError.message || "Metrics could not be loaded.",
                    code: metricsLoadError.code || null,
                    isWarning: true,
                });
            }

            handleClientOrApiAlert(metricsLoadError, "Failed to load metrics");
        }
    }

    async function loadData() {
        try {
            setMessage("");

            const simulationResponse = await authFetch(`/api/simulations/${simulationId}`, token);
            if (!simulationResponse.ok) {
                throw await parseApiError(simulationResponse, "Failed to load simulation");
            }
            const simData = await simulationResponse.json();

            const filesResponse = await authFetch(`/api/files/${simulationId}`, token);
            if (!filesResponse.ok) {
                throw await parseApiError(filesResponse, "Failed to load files");
            }
            const filesData = await filesResponse.json();

            const configResponse = await authFetch(`/api/simulations/${simulationId}/config`, token);
            if (!configResponse.ok) {
                throw await parseApiError(configResponse, "Failed to load configuration");
            }
            const configData = await configResponse.json();

            setSimulation(simData);
            setFiles(filesData);
            setConfig(configData);

            const detectedSimulationFailure = extractSimulationFailure(simData);
            const detectedMetricsFailure = extractMetricsFailure(simData);

            setSimulationFailure(detectedSimulationFailure);
            setMetricsFailure(detectedMetricsFailure);

            await fetchMetricsIfExpected(simData, detectedMetricsFailure);
        } catch (err) {
            const simulationLoadError = normalizeError(err, "Failed to load simulation");

            setMessage(simulationLoadError.message || "Failed to load simulation");
            handleClientOrApiAlert(simulationLoadError, "Failed to load simulation");
        }
    }

    useEffect(() => {
        void loadData();
    }, [simulationId, authFetch, token]);

    const hasActiveSimulation =
        simulation?.status === "CREATED" ||
        simulation?.status === "RUNNING" ||
        simulation?.status === "QUEUED";

    useEffect(() => {
        if (!hasActiveSimulation) return;

        const intervalId = setInterval(() => {
            void loadData();
        }, 3000);

        return () => clearInterval(intervalId);
    }, [hasActiveSimulation, simulationId, authFetch, token]);

    const logFile = files.find(
        (file) => file.fileName === "log.txt" || file.fileType === "LOG"
    );

    const canRunMetrics = !!logFile && !logFile.downloaded;
    const hasMetrics = !!metrics && Object.keys(metrics).length > 0;

    const canRerun = useMemo(() => {
        if (!simulation?.status) return false;

        return (
            simulation.status !== "CREATED" &&
            simulation.status !== "RUNNING" &&
            simulation.status !== "QUEUED"
        );
    }, [simulation]);

    async function handleDownload(file) {
        try {
            const response = await authFetch(file.downloadUrl, token);

            if (!response.ok) {
                throw await parseApiError(response, "Failed to download file");
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);

            const link = document.createElement("a");
            link.href = url;
            link.download = file.fileName;
            document.body.appendChild(link);
            link.click();
            link.remove();

            window.URL.revokeObjectURL(url);

            await loadData();
        } catch (err) {
            const downloadError = normalizeError(err, "Failed to download file");
            handleClientOrApiAlert(downloadError, "Failed to download file");
        }
    }

    async function handleDownloadAll() {
        try {
            const response = await authFetch(`/api/files/${simulationId}/download-all`, token);

            if (!response.ok) {
                throw await parseApiError(response, "Failed to download all files");
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);

            const link = document.createElement("a");
            link.href = url;
            link.download = `simulation-${simulationId}-files.zip`;
            document.body.appendChild(link);
            link.click();
            link.remove();

            window.URL.revokeObjectURL(url);

            await loadData();
        } catch (err) {
            const downloadAllError = normalizeError(err, "Failed to download all files");
            handleClientOrApiAlert(downloadAllError, "Failed to download all files");
        }
    }

    async function handleRunMetrics() {
        if (!canRunMetrics) return;

        setIsRunningMetrics(true);

        try {
            const response = await authFetch(`/api/simulations/${simulationId}/metrics`, token, {
                method: "POST",
            });

            if (!response.ok) {
                throw await parseApiError(response, "Failed to run metrics");
            }

            const metricsData = await response.json();
            setMetrics(metricsData);
            setMetricsFailure(null);
            setIsMetricsOpen(false);

            await loadData();
        } catch (err) {
            const runMetricsError = normalizeError(err, "Failed to run metrics");

            setMetricsFailure({
                title: "Metrics warning",
                message: runMetricsError.message || "Metrics could not be generated.",
                code: runMetricsError.code || null,
                isWarning: true,
            });

            handleClientOrApiAlert(runMetricsError, "Failed to run metrics");
        } finally {
            setIsRunningMetrics(false);
        }
    }

    async function handleRerun() {
        if (!canRerun) return;

        setIsRerunning(true);

        try {
            const response = await authFetch(`/api/simulations/${simulationId}/rerun`, token, {
                method: "POST",
            });

            if (!response.ok) {
                throw await parseApiError(response, "Failed to rerun simulation");
            }

            await response.json();
            navigate("/simulations");
        } catch (err) {
            const rerunError = normalizeError(err, "Failed to rerun simulation");
            handleClientOrApiAlert(rerunError, "Failed to rerun simulation");
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
        simulationFailure,
        metricsFailure,
        isMetricsOpen,
        setIsMetricsOpen,
        isRunningMetrics,
        isRerunning,
        hasMetrics,
        canRunMetrics,
        canRerun,
        handleDownload,
        handleDownloadAll,
        handleRunMetrics,
        handleRerun,
    };
}