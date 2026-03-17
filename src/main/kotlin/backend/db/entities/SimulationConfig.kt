package backend.db.entities

import jakarta.persistence.*

@Entity
@Table(name = "simulation_config")
class SimulationConfig(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "n_groups", nullable = false)
    var nGroups: Int = 0,

    @Column(name = "n_stations", nullable = false)
    var nStations: Int = 0,

    @Column(name = "width", nullable = false)
    var width: Int = 0,

    @Column(name = "height", nullable = false)
    var height: Int = 0,

    @Column(name = "verbosity", nullable = false)
    var verbosity: Int = 0,

    @Column(name = "sim_length", nullable = false)
    var simLength: Long = 0,

    @Column(name = "packet_rate", nullable = false)
    var packetRate: Int = 0,

    @Column(name = "slot_length", nullable = false)
    var slotLength: Long = 0
)