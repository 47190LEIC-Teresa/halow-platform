package backend.db.service

import backend.db.entities.SimulationConfig
import backend.simulator.SimulationRunResult
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class SimulationServiceConfigTest : BaseSimulationServiceTest() {

    @Nested
    inner class ConfigResolution {

        @Test
        fun `should reuse existing config`() {
            val user = createUser()

            configRepo.saveAndFlush(
                SimulationConfig(
                    nGroups = 2,
                    nStations = 10,
                    width = 100,
                    height = 100,
                    verbosity = 1,
                    simLength = 1000,
                    packetRate = 5,
                    slotLength = 50
                )
            )

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                val logFile = File(simulationDir, "log.txt")
                logFile.parentFile.mkdirs()
                logFile.writeText("log")
                SimulationRunResult(0, logFile)
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            assertEquals(1, configRepo.findAll().size)
        }

        @Test
        fun `should create config when no matching config exists`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                val logFile = File(simulationDir, "log.txt")
                logFile.parentFile.mkdirs()
                logFile.writeText("log")
                SimulationRunResult(0, logFile)
            }

            val service = buildService()

            assertEquals(0, configRepo.count())
            service.runSimulation(user.username, params())
            assertEquals(1, configRepo.count())
        }
    }
}