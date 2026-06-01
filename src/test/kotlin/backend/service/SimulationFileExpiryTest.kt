package backend.service

import backend.security.authorization.SimulationSecurity
import backend.exception.SimulationFileMissingForSimulationException
import backend.exception.SimulationFileNoLongerAvailableException
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationFile
import backend.model.entity.User
import backend.model.enums.FileType
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.zip.GZIPOutputStream
import kotlin.test.assertNotNull

class SimulationFileExpiryTest {

    private val fileRepo: SimulationFileRepository = mock()
    private val simulationRepo: SimulationRepository = mock()
    private val simulationSecurity: SimulationSecurity = mock()

    private val simulationFileService = SimulationFileService(
        simulationFileRepository = fileRepo,
        simulationRepository = simulationRepo,
        simulationSecurity = simulationSecurity
    )

    @Test
    fun `should block access when log has expired`() {
        val file = buildLogFile(availableUntil = LocalDateTime.now().minusMinutes(1))

        whenever(fileRepo.findBySimulationIdAndFileType(1L, FileType.LOG)).thenReturn(file)

        val exception = assertThrows<SimulationFileNoLongerAvailableException> {
            simulationFileService.createTempLogFileForSimulation(1L)
        }

        assertNotNull(exception)
    }

    @Test
    fun `should allow access when log has not expired yet`() {
        val file = buildLogFile(availableUntil = LocalDateTime.now().plusMinutes(10))

        whenever(fileRepo.findBySimulationIdAndFileType(1L, FileType.LOG)).thenReturn(file)

        val result = assertDoesNotThrow {
            simulationFileService.createTempLogFileForSimulation(1L)
        }

        assertNotNull(result)
    }

    @Test
    fun `should allow access when log availableUntil is null`() {
        val file = buildLogFile(availableUntil = null)

        whenever(fileRepo.findBySimulationIdAndFileType(1L, FileType.LOG)).thenReturn(file)

        val result = assertDoesNotThrow {
            simulationFileService.createTempLogFileForSimulation(1L)
        }

        assertNotNull(result)
    }

    @Test
    fun `should throw when log file does not exist`() {
        whenever(fileRepo.findBySimulationIdAndFileType(1L, FileType.LOG)).thenReturn(null)

        assertThrows<SimulationFileMissingForSimulationException> {
            simulationFileService.createTempLogFileForSimulation(1L)
        }
    }

    @Test
    fun `should fail when stored log content is not valid gzip`() {
        val file = buildLogFile(
            availableUntil = LocalDateTime.now().plusMinutes(10),
            fileData = byteArrayOf(1, 2, 3)
        )

        whenever(fileRepo.findBySimulationIdAndFileType(1L, FileType.LOG)).thenReturn(file)

        assertThrows<Exception> {
            simulationFileService.createTempLogFileForSimulation(1L)
        }
    }

    private fun gzip(content: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipStream ->
            gzipStream.write(content.toByteArray(Charsets.UTF_8))
        }
        return outputStream.toByteArray()
    }

    private fun buildLogFile(
        availableUntil: LocalDateTime?,
        fileData: ByteArray = gzip("test log content")
    ): SimulationFile {
        val user = User(
            id = 1L,
            username = "john",
            passwordHash = "hash",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )

        val config = SimulationConfig(
            id = 1L,
            nGroups = 2,
            nStations = 10,
            width = 100,
            height = 100,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 5,
            slotLength = 50L
        )

        val simulation = Simulation(
            id = 1L,
            user = user,
            config = config,
            status = SimulationStatus.COMPLETED,
            logStatus = LogStatus.READY,
            label = "Simulation",
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
            parentSimulationId = null
        )

        return SimulationFile(
            id = 10L,
            simulation = simulation,
            fileType = FileType.LOG,
            fileName = "log.gz",
            contentType = "application/gzip",
            fileSize = fileData.size.toLong(),
            downloaded = false,
            downloadedAt = null,
            fileData = fileData,
            availableUntil = availableUntil,
            reminderSentAt = null
        )
    }
}