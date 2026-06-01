import { useAuth } from "../../context/AuthContext";
import { useMetricsUpload } from "./useMetricsUpload";
import MetricsUploadCard from "./components/MetricsUploadCard";
import MetricsSummary from "../../utils/metrics/components/MetricsSummary";
import MetricsOverview from "../../utils/metrics/components/MetricsOverview";
import MetricsDetails from "../../utils/metrics/components/MetricsDetails";
import { FiPlay } from "react-icons/fi";

export default function MetricsPage() {
    const { token, authFetch } = useAuth();

    const {
        selectedFile,
        metrics,
        message,
        isUploading,
        isMetricsOpen,
        setIsMetricsOpen,
        handleFileChange,
        handleUpload,
        resetMetrics,
    } = useMetricsUpload(authFetch, token);

    const hasMetrics = metrics && Object.keys(metrics).length > 0;
    const canRun = !!selectedFile && !isUploading;

    return (
        <>
            <p className="eyebrow">Metrics</p>
            <div className="panel-header-with-action">
                <h2>
                    {hasMetrics ? "Computed results" : "Upload log and compute metrics"}
                </h2>
                    {hasMetrics ? (
                        <button className="secondary-btn" onClick={resetMetrics}>
                            Run another
                        </button>
                    ) : (
                        <button
                            className="run-btn icon-btn"
                            onClick={handleUpload}
                            disabled={!canRun}
                            aria-label={isUploading ? "Processing metrics" : "Compute metrics"}
                            title={isUploading ? "Processing..." : "Compute metrics"}
                        > <FiPlay/>
                        </button>
                    )}
            </div>
            <section className="panel">

                {message && <p className="message">{message}</p>}

                {!hasMetrics ? (
                    <MetricsUploadCard
                        selectedFile={selectedFile}
                        isUploading={isUploading}
                        onFileChange={handleFileChange}
                    />
                ) : (
                    <>
                        <MetricsSummary metrics={metrics}/>
                        <MetricsOverview metrics={metrics}/>
                        <MetricsDetails
                            metrics={metrics}
                            isOpen={isMetricsOpen}
                            onToggle={() => setIsMetricsOpen((prev) => !prev)}
                        />
                    </>
                )}
            </section>
        </>
    );
}