package backend.backend.model.dto

import backend.model.dto.SimulationMetricsResponse

fun parseSimulationMetrics(raw: String): SimulationMetricsResponse {
    fun longValue(pattern: String): Long =
        Regex(pattern, RegexOption.MULTILINE)
            .find(raw)
            ?.groupValues
            ?.get(1)
            ?.toLong()
            ?: throw IllegalArgumentException("Missing metric for pattern: $pattern")

    fun doubleValue(pattern: String): Double =
        Regex(pattern, RegexOption.MULTILINE)
            .find(raw)
            ?.groupValues
            ?.get(1)
            ?.toDouble()
            ?: throw IllegalArgumentException("Missing metric for pattern: $pattern")

    return SimulationMetricsResponse(
        totalPackets = longValue("""Total number of application layer packets:\s*([0-9]+)"""),
        packetsAborted = longValue("""Number of packets that were eventually aborted:\s*([0-9]+)"""),
        packetsReachedMedium = longValue(
            """Number of packets that eventually reached the medium access phase .*:\s*([0-9]+)"""
        ),
        packetsDelivered = longValue(
            """Number of packets that were actually delivered at the receiver's application layer:\s*([0-9]+)"""
        ),
        deliveryRateTotal = doubleValue(
            """Packet delivery rate wrt total number of generated packets:\s*([0-9]+(?:\.[0-9]+)?)"""
        ),
        deliveryRateMedium = doubleValue(
            """Packet delivery rate wrt those that actually reached medium access:\s*([0-9]+(?:\.[0-9]+)?)"""
        ),
        dataFrameTransmissionAttempts = longValue("""Data frame transmission attempts:\s*([0-9]+)"""),
        dataFrameTransmissionSuccesses = longValue(
            """Data frame transmission successes \(only forward direction\):\s*([0-9]+)"""
        ),
        dataFrameAckReceptions = longValue("""Data frame ack receptions \(complete success\):\s*([0-9]+)"""),
        frameDeliveryRateForward = doubleValue("""Frame delivery rate \(forward\):\s*([0-9]+(?:\.[0-9]+)?)"""),
        frameDeliveryRateBackward = doubleValue("""Frame delivery rate \(backward\):\s*([0-9]+(?:\.[0-9]+)?)"""),
        frameDeliveryRateBidirectional = doubleValue(
            """Frame delivery rate \(bidirectional\):\s*([0-9]+(?:\.[0-9]+)?)"""
        ),
        averageDelayUs = doubleValue("""Average:\s*([0-9]+(?:\.[0-9]+)?)\s*us"""),
        delayStdDevUs = doubleValue("""Standard deviation:\s*([0-9]+(?:\.[0-9]+)?)\s*us"""),
        framesWithoutCollision = longValue("""Number of data frames received withOUT collision:\s*([0-9]+)"""),
        framesReceivedWithCollision = longValue("""Number of data frames received even with collision:\s*([0-9]+)"""),
        framesDroppedWithCollision = longValue("""Number of data frames dropped with collision:\s*([0-9]+)"""),
        collidedFramesFraction = doubleValue("""Total fraction of collided data frames:\s*([0-9]+(?:\.[0-9]+)?)""")
    )
}