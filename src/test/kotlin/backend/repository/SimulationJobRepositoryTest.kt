package backend.repository

import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationJob
import backend.model.entity.User
import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationJobRepositoryTest {

    @Autowired
    lateinit var simulationJobRepository: SimulationJobRepository

    @Autowired
    lateinit var simulationRepository: SimulationRepository

    @Autowired
    lateinit var simulationConfigRepository: SimulationConfigRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    private fun tx(): TransactionTemplate = TransactionTemplate(transactionManager)

    @Test
    fun `findUsernamesWithPendingJobs returns empty when there are no pending jobs`() {
        val usernames = simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING)
        assertTrue(usernames.isEmpty())
    }

    @Test
    fun `findUsernamesWithPendingJobs returns distinct sorted usernames`() {
        createPendingJobsForUser("userB", 2, 0)
        createPendingJobsForUser("userA", 1, 10)
        createPendingJobsForUser("userC", 3, 20)

        val usernames = simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING)

        assertEquals(listOf("userA", "userB", "userC"), usernames)
    }

    @Test
    fun `findOldestPendingJobForUserForUpdate returns null when user has no pending jobs`() {
        createPendingJobsForUser("userA", 1, 0)

        val onlyJob = simulationJobRepository.findAll().single()
        onlyJob.status = JobStatus.RUNNING
        simulationJobRepository.saveAndFlush(onlyJob)

        entityManager.clear()

        val result = tx().execute {
            simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                "userA"
            )
        }

        assertNull(result)
    }

    @Test
    fun `findOldestPendingJobForUserForUpdate returns oldest pending job for user`() {
        createPendingJobsForUser("userA", 3, 0)

        entityManager.clear()

        val result = tx().execute {
            simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                "userA"
            )
        } ?: error("Expected a pending job")

        assertEquals("userA", result.simulation.user.username)
        assertEquals("userA-job-0", result.simulation.label)
    }

    @Test
    fun `findOldestPendingJobForUserForUpdate skips first job after status changes`() {
        createPendingJobsForUser("userA", 2, 0)

        entityManager.clear()

        val first = tx().execute {
            val job = simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                "userA"
            ) ?: error("Expected first job")

            job.status = JobStatus.RUNNING
            simulationJobRepository.saveAndFlush(job)
            job
        } ?: error("Expected first job")

        entityManager.clear()

        val second = tx().execute {
            simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                "userA"
            )
        } ?: error("Expected second job")

        assertEquals("userA-job-0", first.simulation.label)
        assertEquals("userA-job-1", second.simulation.label)
    }

    private fun createPendingJobsForUser(
        username: String,
        count: Int,
        startOffsetSeconds: Int
    ) {
        val user = userRepository.findByUsername(username) ?: userRepository.saveAndFlush(
            User(
                username = username,
                passwordHash = "hash",
                email = "$username@example.com",
                firstName = "First",
                lastName = "Last",
                lastAccess = null
            )
        )

        repeat(count) { index ->
            val createdAt = LocalDateTime.now().plusSeconds((startOffsetSeconds + index).toLong())

            val config = simulationConfigRepository.saveAndFlush(
                SimulationConfig(
                    nGroups = 2,
                    nStations = 10,
                    width = 100,
                    height = 100,
                    verbosity = 1,
                    simLength = 1000L,
                    packetRate = 1,
                    slotLength = 1L
                )
            )

            val simulation = simulationRepository.saveAndFlush(
                Simulation(
                    user = user,
                    config = config,
                    status = SimulationStatus.CREATED,
                    logStatus = LogStatus.NOT_READY,
                    label = "$username-job-$index",
                    errorMsg = null,
                    createdAt = createdAt,
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
            )

            simulationJobRepository.saveAndFlush(
                SimulationJob(
                    simulation = simulation,
                    status = JobStatus.PENDING,
                    createdAt = createdAt,
                    startedAt = null,
                    finishedAt = null,
                    errorMsg = null
                )
            )
        }
    }
}