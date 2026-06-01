import { useParams } from "react-router-dom";
import { FiPlay } from "react-icons/fi";
import { VscDebugRerun } from "react-icons/vsc";
import { useAuth } from "../../context/AuthContext";
import { useSimulationDetails } from "./useSimulationDetails";
import { formatFileSize } from "./utils";
import MetricsSummary from "../../utils/metrics/components/MetricsSummary";
import MetricsOverview from "../../utils/metrics/components/MetricsOverview";
import MetricsDetails from "../../utils/metrics/components/MetricsDetails";
import { downloadSimulationCsv } from "../../utils/metrics/metricsCSV";
import { PiFileCsvDuotone } from "react-icons/pi";
import { FaDownload } from "react-icons/fa";

export default function SimulationDetailsPage() {
    const { token, authFetch } = useAuth();
    const { simulationId } = useParams();

    const {
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
    } = useSimulationDetails({
        simulationId,
        authFetch,
        token,
    });

    if (!simulation || !config) {
        return <p>{message || "Loading simulation..."}</p>;
    }

    function handleExportSimulationCsv() {
        downloadSimulationCsv(
            simulation,
            config,
            hasMetrics ? metrics : null,
            simulationId ? `simulation-${simulationId}.csv` : "simulation.csv"
        );
    }

    return (
        <section className="panel">
            <div className="detail-header">
                <h3 style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    Simulation {simulation.label || `SIM-${simulation.simulationId}`}
                    {canRerun && (
                        <button
                            type="button"
                            className="secondary-btn"
                            onClick={handleRerun}
                            disabled={isRerunning}
                            title="Rerun simulation"
                            style={{
                                padding: "0.25rem 0.5rem",
                                background: "cornflowerblue",
                            }}
                        >
                            {isRerunning ? "Rerunning..." : <VscDebugRerun />}
                        </button>
                    )}
                </h3>

                <span className={`status-badge status-${simulation.status.toLowerCase()}`}>
                    {simulation.status}
                </span>
            </div>

            <div className="detail-summary">
                <div>
                    <strong>Owner:</strong>
                    <p>{simulation.owner}</p>
                </div>
                <div>
                    <strong>Created:</strong>
                    <p>{simulation.createdAt}</p>
                </div>
                <div>
                    <strong>Started:</strong>
                    <p>{simulation.startedAt || "—"}</p>
                </div>
                <div>
                    <strong>Finished:</strong>
                    <p>{simulation.finishedAt || "—"}</p>
                </div>
            </div>

            {simulationFailure && (
                <div className="detail-error-card" role="alert">
                    <strong>{simulationFailure.title}</strong>
                    <p>{simulationFailure.message}</p>
                </div>
            )}

            {metricsFailure && (
                <div className="detail-warning-card" role="alert">
                    <strong>{metricsFailure.title}</strong>
                    <p>{metricsFailure.message}</p>
                    {metricsFailure.code && <span>Code: {metricsFailure.code}</span>}
                </div>
            )}

            <div className="detail-grid">
                <div className="detail-card">
                    <div className="panel-header-with-action">
                        <h3>Files</h3>
                        {files.length > 1 && (
                            <button
                                type="button"
                                className="download-btn"
                                onClick={handleDownloadAll}
                                title="Download all files as ZIP"
                            >
                                <FaDownload />
                            </button>
                        )}
                    </div>

                    {files.length > 0 ? (
                        <table className="sim-table">
                            <thead>
                            <tr>
                                <th>Filename</th>
                                <th>Type</th>
                                <th>Size</th>
                                <th>Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            {files.map((file) => {
                                const expiryWarning = getLogExpiryWarning(file);

                                return (
                                    <tr key={file.id}>
                                        <td>
                                            <div
                                                style={{
                                                    display: "flex",
                                                    alignItems: "center",
                                                    gap: "0.5rem",
                                                    flexWrap: "wrap",
                                                }}
                                            >
                                                <span>{file.fileName}</span>

                                                {expiryWarning?.type === "warning" && (
                                                    <span className="file-expiry-badge warning">
                                                            {expiryWarning.text}
                                                        </span>
                                                )}
                                            </div>
                                        </td>

                                        <td>{file.fileType}</td>
                                        <td>{formatFileSize(file.fileSize)}</td>

                                        <td>
                                            {expiryWarning?.type === "expired" ? (
                                                <span className="file-expired-label">Expired</span>
                                            ) : (
                                                <button
                                                    onClick={() => handleDownload(file)}
                                                    className="download-btn"
                                                >
                                                    Download
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    ) : (
                        <p>No files available.</p>
                    )}
                </div>

                <div className="detail-card">
                    <h3
                        style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                        }}
                    >
                        Configuration
                        <button
                            type="button"
                            className="download-btn metrics-export-btn"
                            onClick={handleExportSimulationCsv}
                            title="Export simulation to CSV"
                        >
                            <PiFileCsvDuotone />
                        </button>
                    </h3>

                    <div className="config-list">
                        <ConfigRow label="Seed" value={config.seed} />
                        <ConfigRow label="Stations" value={config.stations} />
                        <ConfigRow label="Groups" value={config.groups} />
                        <ConfigRow label="Height (m)" value={config.height} />
                        <ConfigRow label="Width (m)" value={config.width} />
                        <ConfigRow label="Verbosity" value={config.verbosity} />
                        <ConfigRow label="Simulation Length (us)" value={config.simLength} />
                        <ConfigRow label="Packet Rate (packet/us)" value={config.packetRate} />
                        <ConfigRow label="Slot Length (us)" value={config.slotLength} />
                        <ConfigRow label="Label" value={config.label} />
                    </div>
                </div>
            </div>
            {!simulationFailure && (
                <div className="detail-card">
                    {hasMetrics ? (
                        <>
                            <h3>Metrics</h3>
                            <MetricsSummary metrics={metrics}/>
                            <MetricsOverview metrics={metrics}/>
                            <MetricsDetails
                                metrics={metrics}
                                isOpen={isMetricsOpen}
                                onToggle={() => setIsMetricsOpen((prev) => !prev)}
                            />
                        </>
                    ) : (
                        <>
                            <div className="panel-header-with-action">
                                <h3>Metrics</h3>
                                <button
                                    className="run-btn"
                                    onClick={handleRunMetrics}
                                    disabled={!canRunMetrics || isRunningMetrics}
                                    title={
                                        canRunMetrics
                                            ? "Run metrics"
                                            : "Metrics unavailable until log.txt exists and has not been downloaded"
                                    }
                                >
                                    {isRunningMetrics ? "…" : <FiPlay/>}
                                </button>
                            </div>

                            <p className="helper-text">
                                Metrics have not been generated yet. Run metrics to compute
                                results from the simulation log.
                            </p>
                        </>
                    )}
                </div>
            )}

        </section>
    );
}

function getLogExpiryWarning(file) {
    if (file.fileType !== "LOG" || !file.availableUntil) return null;

    const now = new Date();
    const expiry = new Date(file.availableUntil);

    if (Number.isNaN(expiry.getTime())) return null;

    const diffMs = expiry.getTime() - now.getTime();

    if (diffMs <= 0) {
        return {type: "expired", text: "Expired"};
    }

    const oneHourMs = 60 * 60 * 1000;

    if (diffMs <= oneHourMs) {
        const minutes = Math.ceil(diffMs / (60 * 1000));
        return {type: "warning", text: `Expires in ${minutes} min`};
    }

    return null;
}

function ConfigRow({label, value}) {
    return (
        <div className="config-row">
            <span className="config-label">{label}</span>
            <span className="config-value">{value ?? "—"}</span>
        </div>
    );
}