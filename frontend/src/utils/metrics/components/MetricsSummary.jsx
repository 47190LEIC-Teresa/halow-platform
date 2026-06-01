// src/pages/metrics/components/MetricsSummary.jsx

export default function MetricsSummary({ metrics }) {
    return (
        <div className="stats-grid metrics-summary-grid">
            <div className="stat-card">
                <span>Total Packets</span>
                <strong>{metrics.totalPackets}</strong>
                <p>Generated at application layer</p>
            </div>
            <div className="stat-card">
                <span>Delivered Packets</span>
                <strong>{metrics.packetsDelivered}</strong>
                <p>Successfully delivered</p>
            </div>
            <div className="stat-card">
                <span>Delivery Rate</span>
                <strong>{(metrics.deliveryRateTotal * 100).toFixed(1)}%</strong>
                <p>Relative to total packets</p>
            </div>
            <div className="stat-card">
                <span>Collided Frames Fraction</span>
                <strong>{(metrics.collidedFramesFraction * 100).toFixed(1)}%</strong>
            </div>
            <div className="stat-card">
                <span>Average Delay (us)</span>
                <strong>{metrics.averageDelayUs}</strong>
            </div>
        </div>
    );
}