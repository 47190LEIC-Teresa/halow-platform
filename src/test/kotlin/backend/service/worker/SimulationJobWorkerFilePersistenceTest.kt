package backend.service.worker

import backend.model.entity.Simulation
import backend.model.enums.FileType
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulationJobWorkerFilePersistenceTest : BaseSimulationJobWorkerTest() {

    @Test
    fun `processJob should save log file when runner output contains log file`() {
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

        val files = fileRepo.findAll()
        assertEquals(1, files.size)
        assertEquals(FileType.LOG, files.first().fileType)
    }

    @Test
    fun `processJob should save mp pp pe files when configured`() {
        val user = createUser()
        val config = createConfig()

        val simulation = simulationRepo.saveAndFlush(
            Simulation(
                user = user,
                config = config,
                seed = 123,
                status = backend.model.enums.SimulationStatus.CREATED,
                logStatus = backend.model.enums.LogStatus.NOT_READY,
                createdAt = java.time.LocalDateTime.now(),
                zippedOutput = false,
                mpName = "mp.txt",
                ppName = "pp.txt",
                peName = "pe.txt",
                wGroupFile = false
            )
        )

        val job = createPendingJob(simulation)
        val worker = buildWorker()

        val result = createRunnerResult(exitCode = 0) { dir ->
            java.io.File(dir, "log.txt").writeText("log output")
            java.io.File(dir, "mp.txt").writeText("mp")
            java.io.File(dir, "pp.txt").writeText("pp")
            java.io.File(dir, "pe.txt").writeText("pe")
        }

        every { runner.run(any()) } returns result

        worker.processJob(job.id!!)

        val files = fileRepo.findAll()
        assertEquals(4, files.size)

        val fileTypes = files.map { it.fileType }.toSet()
        assertTrue(fileTypes.contains(FileType.LOG))
        assertTrue(fileTypes.contains(FileType.MP))
        assertTrue(fileTypes.contains(FileType.PP))
        assertTrue(fileTypes.contains(FileType.PE))
    }
}