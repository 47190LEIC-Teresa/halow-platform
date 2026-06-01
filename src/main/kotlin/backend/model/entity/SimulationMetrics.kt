package backend.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "simulation_metrics")
class SimulationMetrics(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(optional = false)
    @JoinColumn(name = "simulation_id")
    var simulation: Simulation,

    // Application layer
    @Column(name = "total_packets", nullable = false)
    var totalPackets: Long = 0,

    @Column(name = "packets_aborted", nullable = false)
    var packetsAborted: Long = 0,

    @Column(name = "packets_reached_medium", nullable = false)
    var packetsReachedMedium: Long = 0,

    @Column(name = "packets_delivered", nullable = false)
    var packetsDelivered: Long = 0,

    @Column(name = "delivery_rate_total", nullable = false)
    var deliveryRateTotal: Double = 0.0,

    @Column(name = "delivery_rate_medium", nullable = false)
    var deliveryRateMedium: Double = 0.0,


    // Link layer
    @Column(name = "tx_attempts", nullable = false)
    var dataFrameTransmissionAttempts: Long = 0,

    @Column(name = "tx_successes", nullable = false)
    var dataFrameTransmissionSuccesses: Long = 0,

    @Column(name = "ack_receptions", nullable = false)
    var dataFrameAckReceptions: Long = 0,

    @Column(name = "frame_rate_forward", nullable = false)
    var frameDeliveryRateForward: Double = 0.0,

    @Column(name = "frame_rate_backward", nullable = false)
    var frameDeliveryRateBackward: Double = 0.0,

    @Column(name = "frame_rate_bidirectional", nullable = false)
    var frameDeliveryRateBidirectional: Double = 0.0,

    // Delay
    @Column(name = "avg_delay_us", nullable = false)
    var averageDelayUs: Double = 0.0,

    @Column(name = "std_delay_us", nullable = false)
    var delayStdDevUs: Double = 0.0,

    // Collisions
    @Column(name = "frames_no_collision", nullable = false)
    var framesWithoutCollision: Long = 0,

    @Column(name = "frames_with_collision", nullable = false)
    var framesReceivedWithCollision: Long = 0,

    @Column(name = "frames_dropped_collision", nullable = false)
    var framesDroppedWithCollision: Long = 0,

    @Column(name = "collision_fraction", nullable = false)
    var collidedFramesFraction: Double = 0.0
)