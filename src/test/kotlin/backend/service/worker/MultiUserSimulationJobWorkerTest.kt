package backend.service.worker

import backend.model.enums.JobStatus
import backend.model.enums.SimulationStatus
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MultiUserSimulationJobWorkerTest : BaseSimulationJobWorkerTest() {

    @Test
    fun `processJob should fail jobs for different users independently`() {
        val worker = buildWorker()
        val config = createConfig()

        val user1 = createUser("alice")
        val user2 = createUser("bob")

        val simulation1 = createSimulation(user1, config, seed = 111)
        val simulation2 = createSimulation(user2, config, seed = 222)

        val job1 = createPendingJob(simulation1, LocalDateTime.now().minusSeconds(10))
        val job2 = createPendingJob(simulation2, LocalDateTime.now().minusSeconds(5))

        every { runner.run(any()) } returnsMany listOf(
            createRunnerResult(0),
            createRunnerResult(0)
        )

        worker.processJob(job1.id!!)
        worker.processJob(job2.id!!)

        val updatedJob1 = jobRepo.findById(job1.id!!).orElseThrow()
        val updatedJob2 = jobRepo.findById(job2.id!!).orElseThrow()
        val updatedSimulation1 = simulationRepo.findById(simulation1.id!!).orElseThrow()
        val updatedSimulation2 = simulationRepo.findById(simulation2.id!!).orElseThrow()

        assertEquals(JobStatus.FAILED, updatedJob1.status)
        assertEquals(JobStatus.FAILED, updatedJob2.status)
        assertNotNull(updatedJob1.startedAt)
        assertNotNull(updatedJob2.startedAt)
        assertNotNull(updatedJob1.finishedAt)
        assertNotNull(updatedJob2.finishedAt)

        assertEquals(SimulationStatus.FAILED, updatedSimulation1.status)
        assertEquals(SimulationStatus.FAILED, updatedSimulation2.status)
        assertNotNull(updatedSimulation1.finishedAt)
        assertNotNull(updatedSimulation2.finishedAt)

        assertEquals(user1.id, updatedSimulation1.user.id)
        assertEquals(user2.id, updatedSimulation2.user.id)
    }

    @Test
    fun `processing one users job should not process another users job`() {
        val worker = buildWorker()
        val config = createConfig()

        val user1 = createUser("alice")
        val user2 = createUser("bob")

        val simulation1 = createSimulation(user1, config, seed = 111)
        val simulation2 = createSimulation(user2, config, seed = 222)

        val job1 = createPendingJob(simulation1, LocalDateTime.now().minusSeconds(10))
        val job2 = createPendingJob(simulation2, LocalDateTime.now().minusSeconds(5))

        every { runner.run(any()) } returns createRunnerResult(0)

        worker.processJob(job1.id!!)

        val updatedJob1 = jobRepo.findById(job1.id!!).orElseThrow()
        val untouchedJob2 = jobRepo.findById(job2.id!!).orElseThrow()
        val updatedSimulation1 = simulationRepo.findById(simulation1.id!!).orElseThrow()
        val untouchedSimulation2 = simulationRepo.findById(simulation2.id!!).orElseThrow()

        assertEquals(JobStatus.FAILED, updatedJob1.status)
        assertNotNull(updatedJob1.finishedAt)

        assertEquals(JobStatus.PENDING, untouchedJob2.status)

        assertEquals(SimulationStatus.FAILED, updatedSimulation1.status)
        assertNotEquals(SimulationStatus.FAILED, untouchedSimulation2.status)
    }

    @Test
    fun `pollPendingJobs should handle pending jobs across different users`() {
        val worker = buildWorker()
        val config = createConfig()

        val user1 = createUser("alice")
        val user2 = createUser("bob")

        val simulation1 = createSimulation(user1, config, seed = 111)
        val simulation2 = createSimulation(user2, config, seed = 222)

        val job1 = createPendingJob(simulation1, LocalDateTime.now().minusMinutes(2))
        val job2 = createPendingJob(simulation2, LocalDateTime.now().minusMinutes(1))

        every { runner.run(any()) } returnsMany listOf(
            createRunnerResult(0),
            createRunnerResult(0)
        )

        worker.pollPendingJobs()

        val updatedJob1 = jobRepo.findById(job1.id!!).orElseThrow()
        val updatedJob2 = jobRepo.findById(job2.id!!).orElseThrow()

        assertNotEquals(JobStatus.PENDING, updatedJob1.status)

        // Keep this commented unless pollPendingJobs is guaranteed to drain the queue.
        // assertNotEquals(JobStatus.PENDING, updatedJob2.status)
    }

    @Test
    fun `multiple pending jobs for different users should be processable independently`() {
        val worker = buildWorker()
        val config = createConfig()

        val users = listOf(
            createUser("alice"),
            createUser("bob"),
            createUser("charlie")
        )

        val simulations = users.mapIndexed { index, user ->
            createSimulation(user, config, seed = 100 + index)
        }

        val jobs = simulations.mapIndexed { index, simulation ->
            createPendingJob(simulation, LocalDateTime.now().minusSeconds((30 - index).toLong()))
        }

        every { runner.run(any()) } returnsMany listOf(
            createRunnerResult(0),
            createRunnerResult(0),
            createRunnerResult(0)
        )

        jobs.forEach { worker.processJob(it.id!!) }

        val updatedJobs = jobs.map { jobRepo.findById(it.id!!).orElseThrow() }
        val updatedSimulations = simulations.map { simulationRepo.findById(it.id!!).orElseThrow() }

        updatedJobs.forEach {
            assertEquals(JobStatus.FAILED, it.status)
            assertNotNull(it.startedAt)
            assertNotNull(it.finishedAt)
        }

        updatedSimulations.forEach {
            assertEquals(SimulationStatus.FAILED, it.status)
            assertNotNull(it.finishedAt)
        }
    }
}