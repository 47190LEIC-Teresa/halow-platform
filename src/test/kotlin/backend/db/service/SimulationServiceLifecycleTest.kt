package backend.db.service

import backend.db.enums.LogStatus
import backend.db.enums.SimulationStatus
import backend.simulator.SimulationRunResult
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class SimulationServiceLifecycleTest : BaseSimulationServiceTest() {

    @Nested
    inner class RunLifecycle {

        @Test
        fun `should run simulation and mark as completed`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                val logFile = File(simulationDir, "log.txt")
                logFile.parentFile.mkdirs()
                logFile.writeText("log")
                SimulationRunResult(0, logFile)
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            val sims = simulationRepo.findAll()
            assertEquals(1, sims.size)

            val sim = sims.first()
            assertEquals(SimulationStatus.COMPLETED, sim.status)
            assertEquals(LogStatus.READY, sim.logStatus)
            assertNotNull(sim.createdAt)
            assertNotNull(sim.startedAt)
            assertNotNull(sim.finishedAt)
        }

        @Test
        fun `should mark simulation as failed when runner fails`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                val logFile = File(simulationDir, "log.txt")
                logFile.parentFile.mkdirs()
                logFile.writeText("log")
                SimulationRunResult(1, logFile)
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            val sim = simulationRepo.findAll().first()
            assertEquals(SimulationStatus.FAILED, sim.status)
            assertEquals(LogStatus.NOT_READY, sim.logStatus)
            assertNotNull(sim.finishedAt)
        }
    }

    @Nested
    inner class Validation {

        @Test
        fun `should throw if user does not exist`() {
            val service = buildService()

            assertThrows(IllegalArgumentException::class.java) {
                service.runSimulation("missing_user", params())
            }
        }
    }

    @Nested
    inner class Flags {

        @Test
        fun `should set output flags correctly in saved simulation`() {
            val user = createUser()

            val request = params().copy(
                mp = "mp.txt",
                pP = "pp.txt",
                pE = "pe.txt",
                fileGroups = "groups.txt",
                zippedOutput = true
            )

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "mp.txt").writeText("mp")
                File(simulationDir, "pp.txt").writeText("pp")
                File(simulationDir, "pe.txt").writeText("pe")
                File(simulationDir, "groups.txt").writeText("groups")
                File(simulationDir, "results.zip").writeText("zip")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, request)

            val sim = simulationRepo.findAll().first()
            assertTrue(sim.wMp)
            assertTrue(sim.wPp)
            assertTrue(sim.wPe)
            assertTrue(sim.wGroupFile)
            assertTrue(sim.zippedOutput)
        }
    }
}