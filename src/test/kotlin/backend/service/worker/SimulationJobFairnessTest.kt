package backend.service.worker

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
import backend.service.EmailService
import backend.service.SimulationFileService
import backend.service.SimulationJobService
import backend.service.SimulationMetricsService
import backend.simulator.ISimulationRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

class SimulationJobFairnessTest {

    private lateinit var simulationJobRepository: SimulationJobRepository
    private lateinit var simulationRepository: SimulationRepository
    private lateinit var simulationRunner: ISimulationRunner
    private lateinit var simulationFileService: SimulationFileService
    private lateinit var simulationMetricsService: SimulationMetricsService
    private lateinit var emailService: EmailService
    private lateinit var transactionTemplate: TransactionTemplate
    private lateinit var schedulerStateRepository: JobSchedulerStateRepository

    private lateinit var service: SimulationJobService

    @BeforeEach
    fun setUp() {
        simulationJobRepository = mock()
        simulationRepository = mock()
        simulationRunner = mock()
        simulationFileService = mock()
        simulationMetricsService = mock()
        emailService = mock()
        transactionTemplate = mock()
        schedulerStateRepository = mock()

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

        whenever(simulationJobRepository.save(any())).thenAnswer {
            it.arguments[0] as SimulationJob
        }
        whenever(schedulerStateRepository.save(any())).thenAnswer {
            it.arguments[0] as JobSchedulerState
        }
    }

    @Test
    fun `claimNextPendingJob alternates fairly between two users`() {
        val schedulerState = buildSchedulerState()

        val aliceJobs = ArrayDeque(
            listOf(
                buildJob(id = 1L, username = "alice"),
                buildJob(id = 2L, username = "alice"),
                buildJob(id = 3L, username = "alice")
            )
        )
        val bobJobs = ArrayDeque(
            listOf(
                buildJob(id = 4L, username = "bob"),
                buildJob(id = 5L, username = "bob"),
                buildJob(id = 6L, username = "bob")
            )
        )

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(schedulerState)
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenAnswer {
                buildPendingUsernames(
                    "alice" to aliceJobs,
                    "bob" to bobJobs
                )
            }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "alice")
        ).thenAnswer {
            if (aliceJobs.isEmpty()) null else aliceJobs.removeFirst()
        }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "bob")
        ).thenAnswer {
            if (bobJobs.isEmpty()) null else bobJobs.removeFirst()
        }

        val claimSequence = mutableListOf<String>()

        repeat(6) {
            val claimed = service.claimNextPendingJob()
            assertNotNull(claimed)
            claimSequence.add(claimed!!.simulation.user.username)
        }

        assertEquals(
            listOf("alice", "bob", "alice", "bob", "alice", "bob"),
            claimSequence
        )
        assertEquals("bob", schedulerState.lastServedUsername)
    }

    @Test
    fun `claimNextPendingJob rotates fairly between three users`() {
        val schedulerState = buildSchedulerState()

        val aliceJobs = ArrayDeque(
            listOf(
                buildJob(id = 1L, username = "alice"),
                buildJob(id = 2L, username = "alice")
            )
        )
        val bobJobs = ArrayDeque(
            listOf(
                buildJob(id = 3L, username = "bob"),
                buildJob(id = 4L, username = "bob")
            )
        )
        val carolJobs = ArrayDeque(
            listOf(
                buildJob(id = 5L, username = "carol"),
                buildJob(id = 6L, username = "carol")
            )
        )

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(schedulerState)
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenAnswer {
                buildPendingUsernames(
                    "alice" to aliceJobs,
                    "bob" to bobJobs,
                    "carol" to carolJobs
                )
            }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "alice")
        ).thenAnswer {
            if (aliceJobs.isEmpty()) null else aliceJobs.removeFirst()
        }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "bob")
        ).thenAnswer {
            if (bobJobs.isEmpty()) null else bobJobs.removeFirst()
        }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "carol")
        ).thenAnswer {
            if (carolJobs.isEmpty()) null else carolJobs.removeFirst()
        }

        val claimSequence = mutableListOf<String>()

        repeat(6) {
            val claimed = service.claimNextPendingJob()
            assertNotNull(claimed)
            claimSequence.add(claimed!!.simulation.user.username)
        }

        assertEquals(
            listOf("alice", "bob", "carol", "alice", "bob", "carol"),
            claimSequence
        )
        assertEquals("carol", schedulerState.lastServedUsername)
    }

    @Test
    fun `claimNextPendingJob skips exhausted user and continues fairly`() {
        val schedulerState = buildSchedulerState()

        val aliceJobs = ArrayDeque(
            listOf(
                buildJob(id = 1L, username = "alice")
            )
        )
        val bobJobs = ArrayDeque(
            listOf(
                buildJob(id = 2L, username = "bob"),
                buildJob(id = 3L, username = "bob"),
                buildJob(id = 4L, username = "bob")
            )
        )
        val carolJobs = ArrayDeque(
            listOf(
                buildJob(id = 5L, username = "carol"),
                buildJob(id = 6L, username = "carol")
            )
        )

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(schedulerState)
        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenAnswer {
                buildPendingUsernames(
                    "alice" to aliceJobs,
                    "bob" to bobJobs,
                    "carol" to carolJobs
                )
            }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "alice")
        ).thenAnswer {
            if (aliceJobs.isEmpty()) null else aliceJobs.removeFirst()
        }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "bob")
        ).thenAnswer {
            if (bobJobs.isEmpty()) null else bobJobs.removeFirst()
        }

        whenever(
            simulationJobRepository.findOldestPendingJobForUserForUpdate(JobStatus.PENDING, "carol")
        ).thenAnswer {
            if (carolJobs.isEmpty()) null else carolJobs.removeFirst()
        }

        val claimSequence = mutableListOf<String>()

        repeat(6) {
            val claimed = service.claimNextPendingJob()
            assertNotNull(claimed)
            claimSequence.add(claimed!!.simulation.user.username)
        }

        assertEquals(
            listOf("alice", "bob", "carol", "bob", "carol", "bob"),
            claimSequence
        )
        assertEquals("bob", schedulerState.lastServedUsername)
    }

    private fun buildPendingUsernames(
        vararg entries: Pair<String, ArrayDeque<SimulationJob>>
    ): List<String> {
        return entries
            .filter { it.second.isNotEmpty() }
            .map { it.first }
            .sorted()
    }

    private fun buildSchedulerState(lastServedUsername: String? = null): JobSchedulerState {
        return JobSchedulerState(
            id = 1L,
            lastServedUsername = lastServedUsername
        )
    }

    private fun buildJob(
        id: Long,
        username: String
    ): SimulationJob {
        val simulation = buildSimulation(id = id, username = username)

        return SimulationJob(
            id = id,
            simulation = simulation,
            status = JobStatus.PENDING,
            createdAt = LocalDateTime.now(),
            startedAt = null,
            finishedAt = null,
            errorMsg = null
        )
    }

    private fun buildSimulation(
        id: Long,
        username: String
    ): Simulation {
        val user = User(
            id = id,
            username = username,
            passwordHash = "hash",
            email = "$username@test.com",
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
            packetRate = 1,
            slotLength = 1L
        )

        return Simulation(
            id = id,
            user = user,
            config = config,
            status = SimulationStatus.CREATED,
            logStatus = LogStatus.NOT_READY,
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
            wMetrics = false,
            zippedOutput = false,
            parentSimulationId = null
        )
    }

    @Test
    fun `claimNextPendingJob remains fair for 100 users`() {
        val schedulerState = buildSchedulerState()

        // 100 users: user_1, user_2, ..., user_100
        val jobQueues: Map<String, ArrayDeque<SimulationJob>> =
            (1..100).associate { idx ->
                val username = "user_$idx"
                // e.g. 5 jobs each
                username to ArrayDeque(
                    (1..5).map { jobIdx ->
                        buildJob(
                            id = idx * 1000L + jobIdx,
                            username = username
                        )
                    }
                )
            }

        whenever(schedulerStateRepository.findByIdForUpdate(1L)).thenReturn(schedulerState)

        whenever(simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING))
            .thenAnswer {
                jobQueues
                    .filter { (_, q) -> q.isNotEmpty() }
                    .keys
                    .sorted()
            }

        jobQueues.keys.forEach { username ->
            whenever(
                simulationJobRepository.findOldestPendingJobForUserForUpdate(
                    JobStatus.PENDING,
                    username
                )
            ).thenAnswer {
                val q = jobQueues[username]!!
                if (q.isEmpty()) null else q.removeFirst()
            }
        }

        val claimCounts = mutableMapOf<String, Int>()
        val totalJobs = jobQueues.values.sumOf { it.size }  // 100 * 5 = 500

        repeat(totalJobs) {
            val claimed = service.claimNextPendingJob()
            // if fairness logic is correct, this should never be null before all jobs are consumed
            assertNotNull(claimed)
            val u = claimed!!.simulation.user.username
            claimCounts[u] = (claimCounts[u] ?: 0) + 1
        }

        // Every user started with 5 jobs, so they should all have 5 claims
        assertEquals(100, claimCounts.size)
        claimCounts.values.forEach { count ->
            assertEquals(5, count)
        }
    }
}