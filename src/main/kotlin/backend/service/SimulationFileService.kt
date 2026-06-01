package backend.service

import backend.model.entity.Simulation
import backend.model.entity.SimulationFile
import backend.model.enums.FileType
import backend.repository.SimulationFileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

@Service
class SimulationFileService(
    private val simulationFileRepository: SimulationFileRepository,
) {
    private val log = LoggerFactory.getLogger(SimulationFileService::class.java)

    fun saveFile(simulation: Simulation, fileType: FileType, file: File, filename: String = ""): SimulationFile {
        if (!file.exists()) {
            throw IllegalArgumentException("$fileType file not found: ${file.name}")
        }

        val detectedContentType = Files.probeContentType(file.toPath())
        val contentType = when {
            file.name.endsWith(".zip", ignoreCase = true) -> "application/zip"
            else -> detectedContentType ?: "application/octet-stream"
        }

        val entity = SimulationFile(
            simulation = simulation,
            fileType = fileType,
            fileName = filename.ifEmpty { file.name },
            contentType = contentType,
            fileSize = file.length(),
            fileData = file.readBytes()
        )

        return simulationFileRepository.save(entity)
    }

    @Transactional(readOnly = true)
    fun getAllFiles(simulationId: Long): List<SimulationFile>
        = simulationFileRepository.findAllBySimulationId(simulationId)

    @Transactional(readOnly = true)
    fun getGroupsFile(simulationId: Long): SimulationFile?
            = simulationFileRepository.findAllBySimulationId(simulationId)
                .firstOrNull { it.fileType == FileType.G }

    @Transactional(readOnly = true)
    fun createTempGroupsFile(simulationId: Long): String? {
        val content = getGroupsFile(simulationId)
            ?: throw IllegalStateException("Simulation $simulationId is missing the group file")

        val temporary = File.createTempFile("simulation_${simulationId}_groups_", ".zip")
        temporary.writeBytes(
            content.fileData ?: throw IllegalStateException("Group file data is missing for simulation $simulationId"))
        temporary.deleteOnExit()
        return temporary.absolutePath
    }

    @Transactional(readOnly = true)
    fun getFileById(id: Long): SimulationFile
        = simulationFileRepository.findById(id)
            .orElseThrow { IllegalArgumentException("File not found: $id") }

    @Transactional
    fun markDownloadedAndClearLog(file: SimulationFile) {
        file.downloaded = true
        file.downloadedAt = LocalDateTime.now()

        if (file.fileType == FileType.LOG) {
            file.fileData = null
        }

        simulationFileRepository.save(file)
    }

    @Transactional(readOnly = true)
    fun createTempLogFileForSimulation(simulationId: Long): File {
        val logFileEntity = simulationFileRepository
            .findBySimulationIdAndFileType(simulationId, FileType.LOG)
            ?: throw IllegalArgumentException("Log file not found for simulation $simulationId")

        if (logFileEntity.downloaded) {
            throw IllegalStateException("Metrics cannot be run because the log file was already downloaded")
        }

        val fileData = logFileEntity.fileData
            ?: throw IllegalStateException("Log file data is missing for simulation $simulationId")

        val tempLogFile = File.createTempFile("simulation_${simulationId}_log_", ".txt")

        // Gzip because first bytes are 1f 8b, which is the magic number for gzip files.
        GZIPInputStream(ByteArrayInputStream(fileData)).use { gzipInput ->
            FileOutputStream(tempLogFile).use { output ->
                val copied = gzipInput.copyTo(output)
                log.info("Extracted gzip log to {} ({} bytes)", tempLogFile.absolutePath, copied)
            }
        }

        return tempLogFile
    }
}
