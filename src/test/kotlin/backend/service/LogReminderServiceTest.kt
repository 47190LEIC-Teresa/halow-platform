package backend.service

import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationFile
import backend.model.entity.User
import backend.model.enums.FileType
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationFileRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class LogReminderServiceTest {

    private val simulationFileRepository: SimulationFileRepository = mock()
    private val emailService: EmailService = mock()
    private val simulationFileService: SimulationFileService = mock()

    private val logReminderService = LogReminderService(
        simulationFileRepository = simulationFileRepository,
        emailService = emailService,
        simulationFileService = simulationFileService
    )

    @Test
    fun `should send reminder for log about to expire`() {
        val file = buildLogFile()

        whenever(simulationFileRepository.findAboutToExpireLogs(any(), any()))
            .thenReturn(listOf(file))

        logReminderService.sendLogExpiryReminders()

        verify(emailService, times(1)).sendLogExpiringReminder(file.simulation, file)
        verify(simulationFileRepository, times(1)).save(file)
        assertNotNull(file.reminderSentAt)
    }

    @Test
    fun `should do nothing when there are no logs to remind`() {
        whenever(simulationFileRepository.findAboutToExpireLogs(any(), any()))
            .thenReturn(emptyList())

        logReminderService.sendLogExpiryReminders()

        verify(emailService, never()).sendLogExpiringReminder(any(), any())
        verify(simulationFileRepository, never()).save(any())
    }

    @Test
    fun `processLogLifecycle should send reminders and clear expired logs`() {
        val file = buildLogFile()

        whenever(simulationFileRepository.findAboutToExpireLogs(any(), any()))
            .thenReturn(listOf(file))

        logReminderService.processLogLifecycle()

        verify(emailService).sendLogExpiringReminder(file.simulation, file)
        verify(simulationFileRepository).save(file)
        verify(simulationFileService).clearExpiredLogFiles()
    }

    private fun buildLogFile(): SimulationFile {
        val user = User(
            id = 1L,
            username = "john",
            passwordHash = "hashed-password",
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe"
        )

        val config = SimulationConfig(
            id = 1L,
            nGroups = 1,
            nStations = 10,
            width = 100,
            height = 100,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 1,
            slotLength = 10L
        )

        val simulation = Simulation(
            id = 1L,
            user = user,
            config = config,
            status = SimulationStatus.COMPLETED,
            logStatus = LogStatus.READY,
            label = "My Simulation",
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
            fileSize = 100,
            downloaded = false,
            downloadedAt = null,
            fileData = byteArrayOf(1, 2, 3),
            availableUntil = LocalDateTime.now().plusMinutes(30),
            reminderSentAt = null
        )
    }
}