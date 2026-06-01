package backend.repository

import backend.model.entity.SimulationFile
import backend.model.enums.FileType
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationFileRepository : JpaRepository<SimulationFile, Long> {
    fun findAllBySimulationId(simulationId: Long): List<SimulationFile>
    fun findBySimulationIdAndFileType(simulationId: Long, fileType: FileType): SimulationFile?
}