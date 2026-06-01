package backend.service.worker

import backend.model.enums.JobStatus
import backend.model.enums.SimulationStatus
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SimulationJobWorkerFailureTest : BaseSimulationJobWorkerTest() {

    @Test
    fun `processJob should mark job and simulation failed when runner throws`() {
        val user = createUser()
        val config = createConfig()
        val simulation = createSimulation(user, config)
        val job = createPendingJob(simulation)
        val worker = buildWorker()

        every { runner.run(any()) } throws RuntimeException("runner exploded")

        worker.processJob(job.id!!)

        val updatedSimulation = simulationRepo.findById(simulation.id!!).get()
        val updatedJob = jobRepo.findById(job.id!!).get()

        assertEquals(SimulationStatus.FAILED, updatedSimulation.status)
        assertEquals(JobStatus.FAILED, updatedJob.status)
        assertNotNull(updatedSimulation.finishedAt)
        assertNotNull(updatedJob.finishedAt)
    }

    @Test
    fun `processJob should mark job and simulation failed when exit code is non zero`() {
        val user = createUser()
        val config = createConfig()
        val simulation = createSimulation(user, config)
        val job = createPendingJob(simulation)
        val worker = buildWorker()

        val result = createRunnerResult(exitCode = 1)
        every { runner.run(any()) } returns result

        worker.processJob(job.id!!)

        val updatedSimulation = simulationRepo.findById(simulation.id!!).get()
        val updatedJob = jobRepo.findById(job.id!!).get()

        assertEquals(SimulationStatus.FAILED, updatedSimulation.status)
        assertEquals(JobStatus.FAILED, updatedJob.status)
        assertNotNull(updatedSimulation.finishedAt)
        assertNotNull(updatedJob.finishedAt)
    }
}