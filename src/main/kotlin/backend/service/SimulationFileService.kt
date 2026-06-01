package backend.service

import backend.security.authorization.SimulationSecurity
import backend.exception.FileUnavailableException
import backend.exception.LogFileExpiredException
import backend.exception.NoFilesAvailableException
import backend.exception.SimulationFileDataMissingException
import backend.exception.SimulationFileMissingException
import backend.exception.SimulationFileMissingForSimulationException
import backend.exception.SimulationFileNoLongerAvailableException
import backend.exception.SimulationFileNotFoundException
import backend.exception.SimulationNotFoundException
import backend.model.dto.FileDownloadData
import backend.model.dto.SimulationFileResponse
import backend.model.dto.ZipDownloadData
import backend.model.dto.ZipEntryData
import backend.model.dto.toFileResponse
import backend.model.entity.Simulation
import backend.model.entity.SimulationFile
import backend.model.enums.FileType
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.zip.GZIPInputStream

@Service
class SimulationFileService(
    private val simulationFileRepository: SimulationFileRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationSecurity: SimulationSecurity
) {
    private val log = LoggerFactory.getLogger(SimulationFileService::class.java)

    fun saveFile(
        simulation: Simulation,
        fileType: FileType,
        file: File,
        filename: String = ""
    ): SimulationFile {
        if (!file.exists()) {
            throw SimulationFileMissingException(fileType, file.name)
        }

        val detectedContentType = Files.probeContentType(file.toPath())
        val contentType = when {
            file.name.endsWith(".zip", ignoreCase = true) -> "application/zip"
            else -> detectedContentType ?: "application/octet-stream"
        }

        val now = LocalDateTime.now()
        val availableFor = 24L

        val entity = SimulationFile(
            simulation = simulation,
            fileType = fileType,
            fileName = filename.ifEmpty { file.name },
            contentType = contentType,
            fileSize = file.length(),
            fileData = file.readBytes(),
            downloadedAt = null,
            availableUntil = if (fileType == FileType.LOG) now.plusHours(availableFor) else null,
            reminderSentAt = null
        )

        return simulationFileRepository.save(entity)
    }

    @Transactional(readOnly = true)
    fun getAllFiles(simulationId: Long): List<SimulationFile> =
        simulationFileRepository.findAllBySimulationId(simulationId)

    @Transactional(readOnly = true)
    fun getGroupsFile(simulationId: Long): SimulationFile? =
        simulationFileRepository.findAllBySimulationId(simulationId)
            .firstOrNull { it.fileType == FileType.G }

    @Transactional(readOnly = true)
    fun createTempGroupsFile(simulationId: Long): String {
        val content = getGroupsFile(simulationId)
            ?: throw SimulationFileMissingForSimulationException(simulationId, FileType.G)

        val temporary = File.createTempFile("simulation_${simulationId}_groups_", ".zip")
        temporary.writeBytes(
            content.fileData ?: throw SimulationFileDataMissingException(simulationId, FileType.G)
        )
        temporary.deleteOnExit()
        return temporary.absolutePath
    }

    @Transactional(readOnly = true)
    fun getFileById(fileId: Long): SimulationFile =
        simulationFileRepository.findById(fileId)
            .orElseThrow { SimulationFileNotFoundException(fileId) }

    @Transactional
    fun markDownloadedFile(file: SimulationFile) {
        file.downloaded = true
        file.downloadedAt = LocalDateTime.now()
    }

    @Transactional
    fun clearExpiredLogFiles() {
        val now = LocalDateTime.now()
        val expiredLogs = simulationFileRepository.findRecentlyExpiredLogsToClear(now)

        log.info("Clearing {} expired log files", expiredLogs.size)

        for (logFile in expiredLogs) {
            log.info("Clearing log file {} for simulation {}", logFile.id, logFile.simulation.id)
            logFile.fileData = null
            logFile.clearedAt = now
        }
    }

    @Transactional(readOnly = true)
    fun createTempLogFileForSimulation(simulationId: Long): File {
        val logFileEntity = simulationFileRepository
            .findBySimulationIdAndFileType(simulationId, FileType.LOG)
            ?: throw SimulationFileMissingForSimulationException(simulationId, FileType.LOG)

        if (
            logFileEntity.availableUntil != null &&
            logFileEntity.availableUntil!!.isBefore(LocalDateTime.now())
        ) {
            throw SimulationFileNoLongerAvailableException(simulationId, FileType.LOG)
        }

        val fileData = logFileEntity.fileData
            ?: throw SimulationFileDataMissingException(simulationId, FileType.LOG)

        val tempLogFile = File.createTempFile("simulation_${simulationId}_log_", ".txt")

        GZIPInputStream(ByteArrayInputStream(fileData)).use { gzipInput ->
            FileOutputStream(tempLogFile).use { output ->
                val copied = gzipInput.copyTo(output)
                log.info("Extracted gzip log to {} ({} bytes)", tempLogFile.absolutePath, copied)
            }
        }

        return tempLogFile
    }

    @Transactional(readOnly = true)
    fun getVisibleFilesForSimulation(simulationId: Long): List<SimulationFileResponse> {
        val simulation = getSimulationEntityById(simulationId)
        val files = simulationFileRepository.findAllBySimulationId(simulationId).toMutableList()

        val hasOwnGroupsFile = files.any { it.fileType == FileType.G }

        if (!hasOwnGroupsFile) {
            simulation.parentSimulationId?.let { parentId ->
                simulationFileRepository.findBySimulationIdAndFileType(parentId, FileType.G)?.let {
                    files.add(it)
                    log.debug("Added inherited groups file from parent simulation {}", parentId)
                }
            }
        }

        log.debug(
            "Files for simulation {}: {}",
            simulationId,
            files.map { "${it.fileName} (${it.fileType})" }
        )

        return files.map(::toFileResponse)
    }

    @Transactional
    fun prepareFileDownload(fileId: Long): FileDownloadData {
        val file = simulationFileRepository.findById(fileId)
            .orElseThrow { SimulationFileNotFoundException(fileId) }

        val data = file.fileData ?: throw FileUnavailableException()
        val now = LocalDateTime.now()

        if (
            file.fileType == FileType.LOG &&
            file.availableUntil != null &&
            file.availableUntil!!.isBefore(now)
        ) {
            throw LogFileExpiredException()
        }

        if (file.fileType == FileType.LOG && !file.downloaded) {
            file.downloaded = true
            file.downloadedAt = now
        }

        return FileDownloadData(
            fileName = file.fileName,
            contentType = file.contentType.ifBlank { "application/octet-stream" },
            data = data
        )
    }

    @Transactional
    fun prepareZipDownload(simulationId: Long): ZipDownloadData {
        val files = getFilesForZipDownload(simulationId)
        val now = LocalDateTime.now()
        val entries = mutableListOf<ZipEntryData>()

        files.forEach { file ->
            val data = file.fileData ?: return@forEach

            if (file.fileType == FileType.LOG) {
                if (file.availableUntil != null && file.availableUntil!!.isBefore(now)) {
                    return@forEach
                }

                if (!file.downloaded) {
                    file.downloaded = true
                    file.downloadedAt = now
                }
            }

            entries.add(
                ZipEntryData(
                    fileName = file.fileName,
                    data = data
                )
            )
        }

        if (entries.isEmpty()) {
            throw NoFilesAvailableException(simulationId)
        }

        return ZipDownloadData(
            fileName = "simulation-$simulationId-files.zip",
            entries = entries
        )
    }

    @Transactional
    fun prepareSelectedZipDownload(
        simulationIds: List<Long>,
        username: String
    ): ZipDownloadData {
        val distinctIds = simulationIds.distinct()

        if (distinctIds.isEmpty()) {
            throw NoFilesAvailableException(-1)
        }

        val now = LocalDateTime.now()
        val allEntries = mutableListOf<ZipEntryData>()

        distinctIds.forEach { simulationId ->
            if (!simulationSecurity.isOwner(simulationId, username)) {
                throw AccessDeniedException("User $username cannot access simulation $simulationId")
            }

            val simulation = getSimulationEntityById(simulationId)
            val files = getFilesForZipDownload(simulationId)

            val collectionFolder = sanitizePathSegment(
                simulation.label?.trim().takeUnless { it.isNullOrBlank() } ?: "single-runs"
            )
            val simulationFolder = "simulation-${simulation.id}"
            val usedNamesInSimulation = mutableSetOf<String>()
            var addedAnyFile = false

            files.forEach { file ->
                val data = file.fileData ?: return@forEach

                if (file.fileType == FileType.LOG) {
                    if (file.availableUntil != null && file.availableUntil!!.isBefore(now)) {
                        return@forEach
                    }

                    if (!file.downloaded) {
                        file.downloaded = true
                        file.downloadedAt = now
                    }
                }

                val safeFileName = uniqueFileNameInFolder(
                    sanitizeFileName(file.fileName),
                    usedNamesInSimulation
                )

                allEntries.add(
                    ZipEntryData(
                        fileName = "$collectionFolder/$simulationFolder/$safeFileName",
                        data = data
                    )
                )

                addedAnyFile = true
            }

            if (!addedAnyFile) {
                log.debug("Skipping simulation {} because no downloadable files were available", simulationId)
            }
        }

        if (allEntries.isEmpty()) {
            throw NoFilesAvailableException(-1)
        }

        return ZipDownloadData(
            fileName = "selected-simulations-files.zip",
            entries = allEntries
        )
    }

    @Transactional(readOnly = true)
    fun getFilesForZipDownload(simulationId: Long): List<SimulationFile> {
        val simulation = getSimulationEntityById(simulationId)
        val files = getAllFiles(simulationId).toMutableList()
        val hasOwnGroupsFile = files.any { it.fileType == FileType.G }

        if (!hasOwnGroupsFile) {
            simulation.parentSimulationId?.let { parentId ->
                getGroupsFile(parentId)?.let(files::add)
            }
        }

        return files
    }

    @Transactional(readOnly = true)
    fun getSimulationEntityById(simulationId: Long): Simulation =
        simulationRepository.findById(simulationId)
            .orElseThrow { SimulationNotFoundException(simulationId) }

    private fun uniqueFileNameInFolder(original: String, usedNames: MutableSet<String>): String {
        if (usedNames.add(original)) return original

        val dotIndex = original.lastIndexOf('.')
        val base = if (dotIndex >= 0) original.substring(0, dotIndex) else original
        val ext = if (dotIndex >= 0) original.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidate = "$base($counter)$ext"
            if (usedNames.add(candidate)) return candidate
            counter++
        }
    }

    private fun sanitizePathSegment(value: String): String =
        value.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), "-")
            .trim('-', '.')
            .ifBlank { "unnamed" }

    private fun sanitizeFileName(value: String): String =
        value.substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
            .ifBlank { "file" }
}