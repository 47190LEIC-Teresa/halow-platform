package backend.model.dto

data class SimulationMetricsResponse(
    val totalPackets: Long,
    val packetsAborted: Long,
    val packetsReachedMedium: Long,
    val packetsDelivered: Long,
    val deliveryRateTotal: Double,
    val deliveryRateMedium: Double,

    val dataFrameTransmissionAttempts: Long,
    val dataFrameTransmissionSuccesses: Long,
    val dataFrameAckReceptions: Long,
    val frameDeliveryRateForward: Double,
    val frameDeliveryRateBackward: Double,
    val frameDeliveryRateBidirectional: Double,

    val averageDelayUs: Double,
    val delayStdDevUs: Double,

    val framesWithoutCollision: Long,
    val framesReceivedWithCollision: Long,
    val framesDroppedWithCollision: Long,
    val collidedFramesFraction: Double
)