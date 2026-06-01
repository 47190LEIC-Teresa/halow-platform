package backend.service.worker

import backend.backend.service.SimulationMetricsService
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationJob
import backend.model.entity.User
import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationConfigRepository
import backend.repository.SimulationFileRepository
import backend.repository.SimulationJobRepository
import backend.repository.SimulationRepository
import backend.repository.UserRepository
import backend.service.SimulationFileService
import backend.service.SimulationJobWorker
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulationRunResult
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class BaseSimulationJobWorkerTest {

    @Autowired
    protected lateinit var userRepo: UserRepository

    @Autowired
    protected lateinit var simulationRepo: SimulationRepository

    @Autowired
    protected lateinit var configRepo: SimulationConfigRepository

    @Autowired
    protected lateinit var fileRepo: SimulationFileRepository

    @Autowired
    protected lateinit var jobRepo: SimulationJobRepository

    protected val runner = mockk<ISimulationRunner>()

    protected fun buildWorker(): SimulationJobWorker {
        val simulationFileService = SimulationFileService(fileRepo)
        val simulationMetricsService = mockk<SimulationMetricsService>()
        return SimulationJobWorker(
            simulationJobRepository = jobRepo,
            simulationRepository = simulationRepo,
            simulationRunner = runner,
            simulationFileService = simulationFileService,
            simulationMetricsService = simulationMetricsService
        )
    }

    protected fun createUser(
        username: String = "testuser",
        email: String = "$username@test.com",
        firstName: String = username,
        lastName: String = "User",
        passwordHash: String = "hash"
    ): User =
        userRepo.saveAndFlush(
            User(
                username = username,
                email = email,
                firstName = firstName,
                lastName = lastName,
                passwordHash = passwordHash
            )
        )

    protected fun createConfig(
        nStations: Int = 10,
        nGroups: Int = 2,
        width: Int = 100,
        height: Int = 100,
        verbosity: Int = 1,
        simLength: Long = 1000L,
        packetRate: Int = 5,
        slotLength: Long = 50L
    ): SimulationConfig =
        configRepo.saveAndFlush(
            SimulationConfig(
                nStations = nStations,
                nGroups = nGroups,
                width = width,
                height = height,
                verbosity = verbosity,
                simLength = simLength,
                packetRate = packetRate,
                slotLength = slotLength
            )
        )

    protected fun createSimulation(
        user: User,
        config: SimulationConfig,
        seed: Int = 123,
        status: SimulationStatus = SimulationStatus.CREATED,
        logStatus: LogStatus = LogStatus.NOT_READY,
        createdAt: LocalDateTime = LocalDateTime.now(),
        zippedOutput: Boolean = false
    ): Simulation =
        simulationRepo.saveAndFlush(
            Simulation(
                user = user,
                config = config,
                status = status,
                logStatus = logStatus,
                createdAt = createdAt,
                seed = seed,
                zippedOutput = zippedOutput,
                mpName = null,
                ppName = null,
                peName = null,
                wGroupFile = false
            )
        )

    protected fun createPendingJob(
        simulation: Simulation,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): SimulationJob =
        createJob(
            simulation = simulation,
            status = JobStatus.PENDING,
            createdAt = createdAt
        )

    protected fun createJob(
        simulation: Simulation,
        status: JobStatus = JobStatus.PENDING,
        createdAt: LocalDateTime = LocalDateTime.now(),
        startedAt: LocalDateTime? = null,
        finishedAt: LocalDateTime? = null
    ): SimulationJob =
        jobRepo.saveAndFlush(
            SimulationJob(
                simulation = simulation,
                status = status,
                createdAt = createdAt,
                startedAt = startedAt,
                finishedAt = finishedAt
            )
        )

    protected fun createRunnerResult(
        exitCode: Int = 0,
        customizeDir: (File) -> Unit = {}
    ): SimulationRunResult {
        val tempDir = File("build/test-output/${System.nanoTime()}").apply {
            mkdirs()
        }

        customizeDir(tempDir)

        return SimulationRunResult(
            exitCode = exitCode,
            stderr = null,
            tempDirectory = tempDir
        )
    }
}