import { useParams } from "react-router-dom";
import { FiPlay } from "react-icons/fi";
import { VscDebugRerun } from "react-icons/vsc";
import { useAuth } from "../../context/AuthContext";
import { useSimulationDetails } from "./useSimulationDetails";
import { formatFileSize } from "./utils";
import MetricsSummary from "../../features/metrics/components/MetricsSummary";
import MetricsOverview from "../../features/metrics/components/MetricsOverview";
import MetricsDetails from "../../features/metrics/components/MetricsDetails";
import {downloadSimulationCsv} from "../../features/metrics/metricsCSV";
import {PiFileCsvDuotone} from "react-icons/pi";

export default function SimulationDetailsPage() {
    const { token, authFetch } = useAuth();
    const { simulationId } = useParams();

    const {
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
            simulationId
                ? `simulation-${simulationId}.csv`
                : "simulation.csv"
        );
    }

    return (
        <>
            <section className="panel">
                <div className="detail-header">
                    <h3
                        style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
                    >
                        Simulation {simulation.label || `SIM-${simulation.simulationId}`}
                        {canRerun && (
                            <button
                                type="button"
                                className="secondary-btn"
                                onClick={ handleRerun }
                                disabled={isRerunning}
                                title="Rerun simulation"
                                style={{
                                    padding: "0.25rem 0.5rem" ,
                                    background: "cornflowerblue"
                                }}
                            >
                                {isRerunning ? "Rerunning..." : < VscDebugRerun />}
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

                <div className="detail-grid">
                    <div className="detail-card">
                        <h3>Files</h3>
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
                                {files.map((file) => (
                                    <tr key={file.id}>
                                        <td>{file.fileName}</td>
                                        <td>{file.fileType}</td>
                                        <td>{formatFileSize(file.fileSize)}</td>
                                        <td>
                                            {file.fileType === "LOG" && file.downloaded ? (
                                                <span className="file-status file-status-used">
                            Downloaded
                          </span>
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
                                ))}
                                </tbody>
                            </table>
                        ) : (
                            <p>No files available.</p>
                        )}
                    </div>

                    <div className="detail-card">
                        <h3 style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItem: "center"
                        }}>Configuration
                            <button
                                type="button"
                                className="download-btn metrics-export-btn"
                                onClick={handleExportSimulationCsv}
                                title="Export simulation to CSV"
                            >
                                <PiFileCsvDuotone/>
                            </button>
                        </h3>
                        <div className="config-list">
                            <ConfigRow label="Seed" value={config.seed}/>
                            <ConfigRow label="Stations" value={config.stations}/>
                            <ConfigRow label="Groups" value={config.groups}/>
                            <ConfigRow label="Height (m)" value={config.height}/>
                            <ConfigRow label="Width (m)" value={config.width}/>
                            <ConfigRow label="Verbosity" value={config.verbosity}/>
                            <ConfigRow label="Simulation Length (us)" value={config.simLength}/>
                            <ConfigRow label="Packet Rate (packet/us)" value={config.packetRate} />
                            <ConfigRow label="Slot Length (us)" value={config.slotLength} />
                            <ConfigRow label="Label" value={config.label} />
                        </div>
                    </div>
                </div>

                <div className="detail-card">
                    {hasMetrics ? (
                        <>
                            <h3>Metrics</h3>
                            <MetricsSummary metrics={metrics} />
                            <MetricsOverview metrics={metrics} />
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
                                    {isRunningMetrics ? "…" : <FiPlay />}
                                </button>
                            </div>

                            <p className="helper-text">
                                Metrics have not been generated yet. Run metrics to compute
                                results from the simulation log.
                            </p>
                        </>
                    )}
                </div>
            </section>
        </>
    );
}

function ConfigRow({ label, value }) {
    return (
        <div className="config-row">
            <span className="config-label">{label}</span>
            <span className="config-value">{value ?? "—"}</span>
        </div>
    );
}