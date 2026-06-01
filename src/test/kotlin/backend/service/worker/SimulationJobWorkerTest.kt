package backend.service.worker

import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SimulationJobWorkerTest : BaseSimulationJobWorkerTest() {

    @Test
    fun `processJob should mark simulation and job completed when runner succeeds`() {
        val user = createUser()
        val config = createConfig()
        val simulation = createSimulation(user, config)
        val job = createPendingJob(simulation)
        val worker = buildWorker()

        val result = createRunnerResult(exitCode = 0) { dir ->
            java.io.File(dir, "log.txt").writeText("log output")
        }

        every { runner.run(any()) } returns result

        worker.processJob(job.id!!)

        val updatedSimulation = simulationRepo.findById(simulation.id!!).get()
        val updatedJob = jobRepo.findById(job.id!!).get()

        assertEquals(SimulationStatus.COMPLETED, updatedSimulation.status)
        assertEquals(LogStatus.READY, updatedSimulation.logStatus)
        assertNotNull(updatedSimulation.startedAt)
        assertNotNull(updatedSimulation.finishedAt)

        assertEquals(JobStatus.COMPLETED, updatedJob.status)
        assertNotNull(updatedJob.startedAt)
        assertNotNull(updatedJob.finishedAt)
    }

    @Test
    fun `markJobRunning should move pending job and simulation to running`() {
        val user = createUser()
        val config = createConfig()
        val simulation = createSimulation(user, config)
        val job = createPendingJob(simulation)
        val worker = buildWorker()

        worker.markJobRunning(job.id!!)

        val updatedSimulation = simulationRepo.findById(simulation.id!!).get()
        val updatedJob = jobRepo.findById(job.id!!).get()

        assertEquals(SimulationStatus.RUNNING, updatedSimulation.status)
        assertEquals(JobStatus.RUNNING, updatedJob.status)
        assertNotNull(updatedSimulation.startedAt)
        assertNotNull(updatedJob.startedAt)
    }
}