
export const metricGroups = [
        "totalPackets",
        "packetsAborted",
        "packetsReachedMedium",
        "packetsDelivered",
        "deliveryRateTotal",
        "deliveryRateMedium",
        "dataFrameTransmissionAttempts",
        "dataFrameTransmissionSuccesses",
        "dataFrameAckReceptions",
        "frameDeliveryRateForward",
        "frameDeliveryRateBackward",
        "frameDeliveryRateBidirectional",
        "averageDelayUs",
        "delayStdDevUs",
        "framesWithoutCollision",
        "framesReceivedWithCollision",
        "framesDroppedWithCollision",
        "collidedFramesFraction"
];

const customLabels = {
    totalPackets: "Total Packets",
    packetsAborted: "Aborted Packets",
    packetsReachedMedium: "Packets Reached Medium",
    packetsDelivered: "Delivered Packets",
    deliveryRateTotal: "Delivery Rate (Total)",
    deliveryRateMedium: "Delivery Rate (Medium)",
    dataFrameTransmissionAttempts: "Transmission Attempts",
    dataFrameTransmissionSuccesses: "Transmission Successes",
    dataFrameAckReceptions: "ACK Receptions",
    frameDeliveryRateForward: "Frame Delivery Rate (Forward)",
    frameDeliveryRateBackward: "Frame Delivery Rate (Backward)",
    frameDeliveryRateBidirectional: "Frame Delivery Rate (Bidirectional)",
    averageDelayUs: "Average Delay (us)",
    delayStdDevUs: "Delay Std Dev (us)",
    framesWithoutCollision: "Frames Without Collision",
    framesReceivedWithCollision: "Frames Received With Collision",
    framesDroppedWithCollision: "Frames Dropped With Collision",
    collidedFramesFraction: "Collided Frames Fraction",
};

const rateFields = [
    "deliveryRateTotal",
    "deliveryRateMedium",
    "frameDeliveryRateForward",
    "frameDeliveryRateBackward",
    "frameDeliveryRateBidirectional",
    "collidedFramesFraction",
];

export function formatMetricName(key) {
    return (
        customLabels[key] ||
        key.replace(/([A-Z])/g, " $1").replace(/^./, (str) => str.toUpperCase())
    );
}

export function formatMetricValue(key, value) {
    if (typeof value !== "number") return value;
    if (rateFields.includes(key)) return `${(value * 100).toFixed(2)}%`;
    if (!Number.isInteger(value)) return value.toFixed(3);
    return value;
}