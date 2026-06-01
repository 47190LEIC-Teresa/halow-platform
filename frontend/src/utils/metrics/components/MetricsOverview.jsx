// src/pages/metrics/components/MetricsOverview.jsx
import MetricBar from "./MetricBar";

export default function MetricsOverview({ metrics }) {
    const collisionTotal =
        metrics.framesWithoutCollision +
        metrics.framesReceivedWithCollision +
        metrics.framesDroppedWithCollision;

    return (
        <div className="detail-card metrics-visual-card">
            <h3>Overview</h3>

            <div className="metrics-visual-section">
                <MetricBar
                    label="Delivered Packets"
                    value={metrics.packetsDelivered}
                    max={metrics.totalPackets}
                />
                <MetricBar
                    label="Aborted Packets"
                    value={metrics.packetsAborted}
                    max={metrics.totalPackets}
                />
                <MetricBar
                    label="Reached Medium"
                    value={metrics.packetsReachedMedium}
                    max={metrics.totalPackets}
                />
            </div>

            <div className="metrics-visual-section">
                <MetricBar
                    label="Delivery Rate (Total)"
                    value={metrics.deliveryRateTotal * 100}
                    max={100}
                    suffix="%"
                    decimals={2}
                />
                <MetricBar
                    label="Delivery Rate (Medium)"
                    value={metrics.deliveryRateMedium * 100}
                    max={100}
                    suffix="%"
                    decimals={2}
                />
                <MetricBar
                    label="Frame Delivery (Bidirectional)"
                    value={metrics.frameDeliveryRateBidirectional * 100}
                    max={100}
                    suffix="%"
                    decimals={2}
                />
            </div>

            <div className="metrics-visual-section">
                <MetricBar
                    label="Frames Without Collision"
                    value={metrics.framesWithoutCollision}
                    max={collisionTotal}
                />
                <MetricBar
                    label="Frames With Collision"
                    value={metrics.framesReceivedWithCollision}
                    max={collisionTotal}
                />
                <MetricBar
                    label="Frames Dropped With Collision"
                    value={metrics.framesDroppedWithCollision}
                    max={collisionTotal}
                />
            </div>
        </div>
    );
}