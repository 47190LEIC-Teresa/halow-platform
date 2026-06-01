package backend.model.entity

import backend.model.enums.LogStatus
import backend.model.enums.MetricsStatus
import backend.model.enums.SimulationStatus
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "simulation")
class Simulation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "username", referencedColumnName = "username")
    var user: User,

    @ManyToOne(optional = false)
    @JoinColumn(name = "config_id")
    var config: SimulationConfig,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: SimulationStatus = SimulationStatus.CREATED,

    @Column(name = "log_status", nullable = false)
    @Enumerated(EnumType.STRING)
    var logStatus: LogStatus = LogStatus.NOT_READY,

    @Column(name = "label")
    var label: String? = null,

    @Column(name = "error_msg", columnDefinition = "TEXT")
    var errorMsg: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "finished_at")
    var finishedAt: LocalDateTime? = null,

    @Column(name = "seed", nullable = false)
    var seed: Int,

    @Column(name = "w_pe")
    var wPe: Boolean = false,

    @Column(name = "w_pp")
    var wPp: Boolean = false,

    @Column(name = "w_mp")
    var wMp: Boolean = false,

    @Column(name = "pe_name")
    var peName: String? = null,

    @Column(name = "pp_name")
    var ppName: String? = null,

    @Column(name = "mp_name")
    var mpName: String? = null,

    @Column(name = "w_group_file")
    var wGroupFile: Boolean = false,

    @Column(name = "w_metrics")
    var wMetrics: Boolean = false,

    @Column(name = "zipped_output")
    var zippedOutput: Boolean = false,

    @Column(name = "parent_simulation_id")
    var parentSimulationId: Long? = null,

    @Enumerated(EnumType.STRING)
    var metricsStatus: MetricsStatus = MetricsStatus.NOT_REQUESTED,

    @Column(columnDefinition = "TEXT")
    var metricsErrorMsg: String? = null,
)