package backend.db.entities

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

    @Column(name = "total_packets", nullable = false)
    var totalPackets: Int = 0,

    @Column(name = "packets_aborted", nullable = false)
    var packetsAborted: Int = 0,

    @Column(name = "packets_reached_medium", nullable = false)
    var packetsReachedMedium: Int = 0,

    @Column(name = "delivery_rate_total", nullable = false)
    var deliveryRateTotal: Double = 0.0,

    @Column(name = "delivery_rate_medium", nullable = false)
    var deliveryRateMedium: Double = 0.0

)