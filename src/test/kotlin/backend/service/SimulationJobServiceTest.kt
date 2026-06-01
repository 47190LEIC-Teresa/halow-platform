package backend.service

import backend.exception.JobSchedulerStateMissingException
import backend.model.entity.JobSchedulerState
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationJob
import backend.model.entity.User
import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.JobSchedulerStateRepository
import backend.repository.SimulationJobRepository
import backend.repository.SimulationRepository
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulationRunResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.io.File
import java.time.LocalDateTime
import java.util.Optional
import kotlin.io.path.createTempDirectory

class SimulationJobServiceTest {

    private lateinit var simulationJobRepository: SimulationJobRepository
    private lateinit var simulationRepository: SimulationRepository
    private lateinit var schedulerStateRepository: JobSchedulerStateRepository
    private lateinit var simulationRunner: ISimulationRunner
    private lateinit var simulationFileService: SimulationFileService
    private lateinit var simulationMetricsService: SimulationMetricsService
    private lateinit var emailService: EmailService
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var service: SimulationJobService

    @BeforeEach
    fun setUp() {
        simulationJobRepository = mock()
        simulationRepository = mock()
        schedulerStateRepository = mock()
        simulationRunner = mock()
        simulationFileService = mock()
        simulationMetricsService = mock()
        emailService = mock()
        transactionTemplate = mock()

        whenever(transactionTemplate.execute<Simulation>(any()))
            .thenAnswer { invocation ->
                val callback = invocation.getArgument<org.springframework.transaction.support.TransactionCallback<Simulation>>(0)
                callback.doInTransaction(SimpleTransactionStatus())
            }

        service = SimulationJobService(
            simulationJobRepository = simulationJobRepository,
            simulationRepository = simulationRepository,
            schedulerStateRepository = schedulerStateRepository,
            simulationRunner = simulationRunner,
            simulationFileService = simulationFileService,
            simulationMetricsService = simulationMetricsService,
            emailService = emailService,
            transactionTemplate = transactionTemplate
        )
    }

    @Test
    fun `createJob should save pending job`() {
        val simulation = buildSimulation()
        val savedJob = buildJob(simulation = simulation, status = JobStatus.PENDING)

        whenever(simulationJobRepository.save(any())).thenReturn(savedJob)

        val result = service.createJob(simulation)

        assertEquals(JobStatus.PENDING, result.status)
        assertEquals(simulation, result.simulation)
        verify(simulationJobRepository).save(any())
    }

    @Test
    fun `claimNextPendingJob returns null when no pending job exists`() {
        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(buildSchedulerState())
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING)).thenReturn(emptyList())

        val result = service.claimNextPendingJob()

        assertNull(result)
        verify(simulationJobRepository, never()).findOldestPendingJobForUserForUpdate(any(), any())
    }

    @Test
    fun `claimNextPendingJob throws when scheduler state row is missing`() {
        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(null)

        assertThrows(JobSchedulerStateMissingException::class.java) {
            service.claimNextPendingJob()
        }
    }

    @Test
    fun `claimNextPendingJob marks job and simulation as running`() {
        val simulation = buildSimulation(status = SimulationStatus.CREATED)
        val job = buildJob(simulation = simulation, status = JobStatus.PENDING)

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(buildSchedulerState())
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenReturn(listOf(simulation.user.username))
        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                simulation.user.username
            )
        ).thenReturn(job)
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        val result = service.claimNextPendingJob()

        assertNotNull(result)
        assertEquals(JobStatus.RUNNING, job.status)
        assertEquals(SimulationStatus.RUNNING, simulation.status)
        assertNotNull(job.startedAt)
        assertNotNull(simulation.startedAt)
        verify(schedulerStateRepository).save(any())
        verify(simulationJobRepository).save(job)
    }

    @Test
    fun `claimNextPendingJob should use next username after last served`() {
        val userAJob = buildJob(simulation = buildSimulation(id = 1L), status = JobStatus.PENDING)
        val state = buildSchedulerState(lastServedUsername = "user1")

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(state)
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenReturn(listOf("user1", "user2"))
        whenever(simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "user2"))
            .thenReturn(userAJob)
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.claimNextPendingJob()

        verify(simulationJobRepository).findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "user2")
    }

    @Test
    fun `processJob returns immediately when job is not running`() {
        val simulation = buildSimulation(status = SimulationStatus.CREATED)
        val job = buildJob(id = 23L, simulation = simulation, status = JobStatus.PENDING)

        whenever(simulationJobRepository.findById(23L)).thenReturn(Optional.of(job))

        service.processJob(23L)

        verify(simulationRunner, never()).run(any())
        verify(emailService, never()).sendSimulationFinishedEmail(any())
    }

    @Test
    fun `processJob with successful result marks completed and sends email`() {
        val simulation = buildSimulation(
            id = 10L,
            status = SimulationStatus.RUNNING,
            logStatus = LogStatus.NOT_READY,
            wMetrics = false
        )
        val job = buildJob(id = 20L, simulation = simulation, status = JobStatus.RUNNING)
        val tempDir = createTempDirectory("sim-job-success").toFile()
        File(tempDir, "log.txt").writeText("ok")

        whenever(simulationJobRepository.findById(20L)).thenReturn(Optional.of(job))
        whenever(simulationRunner.run(any())).thenReturn(
            SimulationRunResult(
                exitCode = 0,
                stderr = "",
                tempDirectory = tempDir
            )
        )
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.processJob(20L)

        assertEquals(JobStatus.COMPLETED, job.status)
        assertEquals(SimulationStatus.COMPLETED, simulation.status)
        assertEquals(LogStatus.READY, simulation.logStatus)
        assertNotNull(job.finishedAt)
        assertNotNull(simulation.finishedAt)
        verify(simulationFileService).saveFile(
            eq(simulation),
            eq(backend.model.enums.FileType.LOG),
            any<File>(),
            any<String>()
        )
        verify(emailService).sendSimulationFinishedEmail(simulation)
        verify(simulationMetricsService, never()).runSimulation(any())
    }

    @Test
    fun `processJob successful result should trigger metrics when enabled`() {
        val simulation = buildSimulation(
            id = 30L,
            status = SimulationStatus.RUNNING,
            logStatus = LogStatus.NOT_READY,
            wMetrics = true
        )
        val job = buildJob(id = 31L, simulation = simulation, status = JobStatus.RUNNING)
        val tempDir = createTempDirectory("sim-job-metrics").toFile()
        File(tempDir, "log.txt").writeText("ok")

        whenever(simulationJobRepository.findById(31L)).thenReturn(Optional.of(job))
        whenever(simulationRunner.run(any())).thenReturn(
            SimulationRunResult(
                exitCode = 0,
                stderr = "",
                tempDirectory = tempDir
            )
        )
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.processJob(31L)

        verify(simulationMetricsService).runSimulation(30L)
        verify(emailService).sendSimulationFinishedEmail(simulation)
    }

    @Test
    fun `processJob with non zero exit code marks failed and sends email`() {
        val simulation = buildSimulation(
            id = 11L,
            status = SimulationStatus.RUNNING,
            logStatus = LogStatus.NOT_READY
        )
        val job = buildJob(id = 21L, simulation = simulation, status = JobStatus.RUNNING)
        val tempDir = createTempDirectory("sim-job-fail-exit").toFile()

        whenever(simulationJobRepository.findById(21L)).thenReturn(Optional.of(job))
        whenever(simulationRunner.run(any())).thenReturn(
            SimulationRunResult(
                exitCode = 42,
                stderr = "boom",
                tempDirectory = tempDir
            )
        )
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.processJob(21L)

        assertEquals(JobStatus.FAILED, job.status)
        assertEquals(SimulationStatus.FAILED, simulation.status)
        assertEquals(LogStatus.NOT_READY, simulation.logStatus)
        assertEquals("Simulation failed (exit code 42): boom", simulation.errorMsg)
        assertEquals("Simulation job failed with exit code 42", job.errorMsg)
        verify(emailService).sendSimulationFinishedEmail(simulation)
    }

    @Test
    fun `processJob when runner throws marks failed and sends email`() {
        val simulation = buildSimulation(
            id = 12L,
            status = SimulationStatus.RUNNING,
            logStatus = LogStatus.NOT_READY
        )
        val job = buildJob(id = 22L, simulation = simulation, status = JobStatus.RUNNING)

        whenever(simulationJobRepository.findById(22L)).thenReturn(Optional.of(job))
        whenever(simulationRunner.run(any())).thenThrow(RuntimeException("runner crashed"))
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.processJob(22L)

        assertEquals(JobStatus.FAILED, job.status)
        assertEquals(SimulationStatus.FAILED, simulation.status)
        assertEquals("runner crashed", simulation.errorMsg)
        assertEquals("runner crashed", job.errorMsg)
        verify(emailService).sendSimulationFinishedEmail(simulation)
    }

    @Test
    fun `processJob for one user does not affect another users job`() {
        val sim1 = buildSimulation(id = 100L, status = SimulationStatus.RUNNING)
        val sim2 = buildSimulation(id = 200L, status = SimulationStatus.CREATED)

        val job1 = buildJob(id = 1L, simulation = sim1, status = JobStatus.RUNNING)
        val job2 = buildJob(id = 2L, simulation = sim2, status = JobStatus.PENDING)

        val tempDir = createTempDirectory("sim-user-a").toFile()
        File(tempDir, "log.txt").writeText("ok")

        whenever(simulationJobRepository.findById(1L)).thenReturn(Optional.of(job1))
        whenever(simulationRunner.run(any())).thenReturn(
            SimulationRunResult(
                exitCode = 0,
                stderr = "",
                tempDirectory = tempDir
            )
        )
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationJobRepository.save(any())).thenAnswer { it.arguments[0] as SimulationJob }

        service.processJob(1L)

        assertEquals(SimulationStatus.COMPLETED, sim1.status)
        assertEquals(JobStatus.COMPLETED, job1.status)

        assertEquals(SimulationStatus.CREATED, sim2.status)
        assertEquals(JobStatus.PENDING, job2.status)
    }

    private fun buildSchedulerState(lastServedUsername: String? = null): JobSchedulerState {
        return JobSchedulerState(
            id = 1L,
            lastServedUsername = lastServedUsername
        )
    }

    private fun buildJob(
        id: Long = 1L,
        simulation: Simulation,
        status: JobStatus = JobStatus.PENDING
    ): SimulationJob {
        return SimulationJob(
            id = id,
            simulation = simulation,
            status = status,
            createdAt = LocalDateTime.now(),
            startedAt = null,
            finishedAt = null,
            errorMsg = null
        )
    }

    private fun buildSimulation(
        id: Long = 1L,
        status: SimulationStatus = SimulationStatus.CREATED,
        logStatus: LogStatus = LogStatus.NOT_READY,
        wMetrics: Boolean = false
    ): Simulation {
        val user = User(
            id = id,
            username = "user$id",
            passwordHash = "hash",
            email = "user$id@example.com",
            firstName = "User",
            lastName = "$id",
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
            packetRate = 1,
            slotLength = 1L
        )

        return Simulation(
            id = id,
            user = user,
            config = config,
            status = status,
            logStatus = logStatus,
            label = "sim-$id",
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
            wMetrics = wMetrics,
            zippedOutput = false,
            parentSimulationId = null
        )
    }
}