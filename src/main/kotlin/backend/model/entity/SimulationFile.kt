package backend.model.entity

import backend.model.enums.FileType
import jakarta.persistence.*
import java.time.LocalDateTime

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

    @Column(name = "content_type", nullable = false)    //binary content
    var contentType: String,

    @Column(name = "file_size")
    var fileSize: Long,

    @Column(name = "downloaded", nullable = false)
    var downloaded: Boolean = false,

    @Column(name = "downloaded_at")
    var downloadedAt: LocalDateTime? = null,

    @Column(name = "available_until")
    var availableUntil: LocalDateTime? = null,

    @Column(name = "reminder_sent_at")
    var reminderSentAt: LocalDateTime? = null,

    @Column(name = "cleared_at")
    var clearedAt: LocalDateTime? = null,

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data")     // Store file content as a byte array
    var fileData: ByteArray?        // nullable to allow clearing log data after download
)