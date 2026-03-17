package backend.db.entities

import backend.db.enums.FileType
import jakarta.persistence.*

@Entity
@Table(name = "simulation_file")
class SimulationFile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "simulation_id")
    var simulation: Simulation,

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var fileType: FileType,

    @Column(name = "file_name", nullable = false)
    var fileName: String,

    @Column(name = "file_url", nullable = false)
    var fileUrl: String,

    @Column(name = "file_size")
    var fileSize: Long
)