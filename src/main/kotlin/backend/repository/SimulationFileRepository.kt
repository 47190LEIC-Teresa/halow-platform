package backend.repository

import backend.model.entity.SimulationFile
import backend.model.enums.FileType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface SimulationFileRepository : JpaRepository<SimulationFile, Long> {
    fun findAllBySimulationId(simulationId: Long): List<SimulationFile>
    fun findBySimulationIdAndFileType(simulationId: Long, fileType: FileType): SimulationFile?
    @Query("""
        select f
        from SimulationFile f
        where f.fileType = backend.model.enums.FileType.LOG
          and f.downloaded = false
          and f.reminderSentAt is null
          and f.availableUntil between :from and :to
    """)
    fun findAboutToExpireLogs(from: LocalDateTime,  to: LocalDateTime ): List<SimulationFile>

    @Query("""
    select f
    from SimulationFile f
    where f.fileType = backend.model.enums.FileType.LOG
      and f.availableUntil < :now
      and f.clearedAt is null
""")
    fun findRecentlyExpiredLogsToClear(now: LocalDateTime): List<SimulationFile>

}