// src/pages/metrics/components/MetricsDetails.jsx
import { metricGroups, formatMetricName, formatMetricValue } from "../metricsFormatters";
import { downloadMetricsCsv } from "../metricsCSV";
import { PiFileCsvDuotone } from "react-icons/pi";

export default function MetricsDetails({ metrics, isOpen, onToggle, simulationId }) {
    function handleExportCsv() {
        downloadMetricsCsv(
            metrics,
            simulationId
                ? `simulation-${simulationId}-metrics.csv`
                : "metrics.csv"
        );
    }

    return (

    <div className="metrics-details-section">
        <div className="metrics-actions">
            <button
                type="button"
                className="download-btn metrics-export-btn"
                onClick={handleExportCsv}
                disabled={!metrics}
                title="Export metrics to CSV"
            >
                <PiFileCsvDuotone/>

            </button>
            <button
                type="button"
                className="collapse-toggle"
                onClick={onToggle}
                aria-expanded={isOpen}
            >
                <span>{isOpen ? "Hide full details" : "See full details"}</span>
                <span>{isOpen ? "−" : "+"}</span>
            </button>
        </div>

        {isOpen && (
            <div className="metrics-details-panel">
                {Object.entries(metricGroups).map(([groupName, keys]) => (
                    <div className="metrics-group" key={groupName}>
                            <h3>{groupName}</h3>
                            <div className="config-list">
                                {keys.map((key) => (
                                    <div className="config-row" key={key}>
                                        <span className="config-label">
                                            {formatMetricName(key)}
                                        </span>
                                        <span className="config-value">
                                            {formatMetricValue(key, metrics[key])}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );

}