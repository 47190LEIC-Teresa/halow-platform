package backend.service

import backend.security.authorization.SimulationSecurity
import backend.exception.LogFileExpiredException
import backend.exception.NoFilesAvailableException
import backend.exception.SimulationFileDataMissingException
import backend.exception.SimulationFileMissingException
import backend.exception.SimulationFileMissingForSimulationException
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationFile
import backend.model.entity.User
import backend.model.enums.FileType
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.io.File
import java.time.LocalDateTime
import java.util.Optional

class SimulationFileServiceTest {

    private lateinit var simulationFileRepository: SimulationFileRepository
    private lateinit var simulationRepository: SimulationRepository
    private lateinit var simulationSecurity: SimulationSecurity
    private lateinit var service: SimulationFileService

    @BeforeEach
    fun setUp() {
        simulationFileRepository = mock()
        simulationRepository = mock()
        simulationSecurity = mock()
        service = SimulationFileService(
            simulationFileRepository = simulationFileRepository,
            simulationRepository = simulationRepository,
            simulationSecurity = simulationSecurity
        )
    }

    @Test
    fun `saveFile should throw when source file does not exist`() {
        val simulation = buildSimulation()
        val file = File("does-not-exist.txt")

        assertThrows(SimulationFileMissingException::class.java) {
            service.saveFile(simulation, FileType.LOG, file)
        }
    }

    @Test
    fun `saveFile should store log file metadata`() {
        val simulation = buildSimulation()
        val tempFile = kotlin.io.path.createTempFile("log", ".txt").toFile().apply {
            writeText("simulation log")
        }

        whenever(simulationFileRepository.save(any())).thenAnswer { it.arguments[0] as SimulationFile }

        val saved = service.saveFile(simulation, FileType.LOG, tempFile, "log.txt")

        assertEquals(FileType.LOG, saved.fileType)
        assertEquals("log.txt", saved.fileName)
        assertNotNull(saved.availableUntil)
        assertEquals(simulation, saved.simulation)
    }

    @Test
    fun `saveFile should store non log file without expiry`() {
        val simulation = buildSimulation()
        val tempFile = kotlin.io.path.createTempFile("groups", ".zip").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }

        whenever(simulationFileRepository.save(any())).thenAnswer { it.arguments[0] as SimulationFile }

        val saved = service.saveFile(simulation, FileType.G, tempFile, "groups.zip")

        assertEquals(FileType.G, saved.fileType)
        assertNull(saved.availableUntil)
    }

    @Test
    fun `createTempGroupsFile should throw when groups file missing`() {
        whenever(simulationFileRepository.findAllBySimulationId(1L)).thenReturn(emptyList())

        assertThrows(SimulationFileMissingForSimulationException::class.java) {
            service.createTempGroupsFile(1L)
        }
    }

    @Test
    fun `createTempGroupsFile should throw when groups data missing`() {
        whenever(simulationFileRepository.findAllBySimulationId(1L))
            .thenReturn(listOf(buildFile(fileType = FileType.G, fileData = null)))

        assertThrows(SimulationFileDataMissingException::class.java) {
            service.createTempGroupsFile(1L)
        }
    }

    @Test
    fun `clearExpiredLogFiles should clear file data and set clearedAt`() {
        val expired = buildFile(fileType = FileType.LOG, fileData = byteArrayOf(1, 2, 3))

        whenever(simulationFileRepository.findRecentlyExpiredLogsToClear(any()))
            .thenReturn(listOf(expired))

        service.clearExpiredLogFiles()

        assertNull(expired.fileData)
        assertNotNull(expired.clearedAt)
    }

    @Test
    fun `prepareFileDownload should mark log downloaded`() {
        val file = buildFile(fileType = FileType.LOG)

        whenever(simulationFileRepository.findById(10L)).thenReturn(Optional.of(file))

        val result = service.prepareFileDownload(10L)

        assertEquals(file.fileName, result.fileName)
        assertEquals(file.fileData, result.data)
        assertEquals(true, file.downloaded)
        assertNotNull(file.downloadedAt)
    }

    @Test
    fun `prepareFileDownload should throw for expired log`() {
        val file = buildFile(
            fileType = FileType.LOG,
            availableUntil = LocalDateTime.now().minusMinutes(1)
        )

        whenever(simulationFileRepository.findById(10L)).thenReturn(Optional.of(file))

        assertThrows(LogFileExpiredException::class.java) {
            service.prepareFileDownload(10L)
        }
    }

    @Test
    fun `getVisibleFilesForSimulation should include inherited parent groups file`() {
        val child = buildSimulation(id = 2L, parentSimulationId = 1L)

        whenever(simulationRepository.findById(2L)).thenReturn(Optional.of(child))
        whenever(simulationFileRepository.findAllBySimulationId(2L))
            .thenReturn(listOf(buildFile(id = 20L, simulation = child, fileType = FileType.LOG)))
        whenever(simulationFileRepository.findBySimulationIdAndFileType(1L, FileType.G))
            .thenReturn(buildFile(id = 21L, simulation = buildSimulation(id = 1L), fileType = FileType.G))

        val files = service.getVisibleFilesForSimulation(2L)

        assertEquals(2, files.size)
    }

    @Test
    fun `prepareZipDownload should throw when nothing is downloadable`() {
        val simulation = buildSimulation(id = 5L)
        val expiredLog = buildFile(
            simulation = simulation,
            fileType = FileType.LOG,
            availableUntil = LocalDateTime.now().minusMinutes(5)
        )

        whenever(simulationRepository.findById(5L)).thenReturn(Optional.of(simulation))
        whenever(simulationFileRepository.findAllBySimulationId(5L)).thenReturn(listOf(expiredLog))

        assertThrows(NoFilesAvailableException::class.java) {
            service.prepareZipDownload(5L)
        }
    }

    @Test
    fun `prepareSelectedZipDownload should throw when user is not owner`() {
        whenever(simulationSecurity.isOwner(1L, "alice")).thenReturn(false)

        assertThrows(AccessDeniedException::class.java) {
            service.prepareSelectedZipDownload(listOf(1L), "alice")
        }
    }

    @Test
    fun `prepareSelectedZipDownload should return entries when user owns simulations`() {
        val simulation = buildSimulation(id = 1L, label = "Batch 1")
        val file = buildFile(simulation = simulation, fileType = FileType.LOG)

        whenever(simulationSecurity.isOwner(1L, "alice")).thenReturn(true)
        whenever(simulationRepository.findById(1L)).thenReturn(Optional.of(simulation))
        whenever(simulationFileRepository.findAllBySimulationId(1L)).thenReturn(listOf(file))

        val result = service.prepareSelectedZipDownload(listOf(1L), "alice")

        assertEquals("selected-simulations-files.zip", result.fileName)
        assertEquals(1, result.entries.size)
    }

    private fun buildSimulation(
        id: Long = 1L,
        label: String = "Simulation",
        parentSimulationId: Long? = null
    ): Simulation {
        val user = User(
            id = id,
            username = "user$id",
            passwordHash = "hash",
            email = "user$id@test.com",
            firstName = "User",
            lastName = "Test",
            lastAccess = null
        )

        val config = SimulationConfig(
            id = id,
            nGroups = 2,
            nStations = 10,
            width = 100,
            height = 100,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 5,
            slotLength = 50L
        )

        return Simulation(
            id = id,
            user = user,
            config = config,
            status = SimulationStatus.COMPLETED,
            logStatus = LogStatus.READY,
            label = label,
            errorMsg = null,
            createdAt = LocalDateTime.now(),
            startedAt = null,
            finishedAt = null,
            seed = 123,
            wPe = false,
            wPp = false,
            wMp = false,
            peName = null,
            ppName = null,
            mpName = null,
            wGroupFile = false,
            wMetrics = false,
            zippedOutput = false,
            parentSimulationId = parentSimulationId
        )
    }

    private fun buildFile(
        id: Long = 10L,
        simulation: Simulation = buildSimulation(),
        fileType: FileType = FileType.LOG,
        fileData: ByteArray? = byteArrayOf(1, 2, 3),
        availableUntil: LocalDateTime? = LocalDateTime.now().plusHours(1)
    ): SimulationFile {
        return SimulationFile(
            id = id,
            simulation = simulation,
            fileType = fileType,
            fileName = if (fileType == FileType.LOG) "log.gz" else "file.bin",
            contentType = "application/octet-stream",
            fileSize = fileData?.size?.toLong() ?: 0L,
            downloaded = false,
            downloadedAt = null,
            fileData = fileData,
            availableUntil = availableUntil,
            reminderSentAt = null
        )
    }
}