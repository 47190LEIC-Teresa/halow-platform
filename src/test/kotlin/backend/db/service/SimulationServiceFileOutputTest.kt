package backend.db.service

import backend.db.enums.FileType
import backend.simulator.SimulationRunResult
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class SimulationServiceFileOutputTest : BaseSimulationServiceTest() {

    @Nested
    inner class LogFile {

        @Test
        fun `should save log file metadata correctly`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                val logFile = File(simulationDir, "log.txt")
                logFile.parentFile.mkdirs()
                logFile.writeText("simulation log")
                SimulationRunResult(0, logFile)
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            val files = fileRepo.findAll()
            assertEquals(1, files.size)

            val file = files.first()
            assertEquals(FileType.LOG, file.fileType)
            assertEquals("log.txt", file.fileName)
            assertTrue(file.fileUrl.contains("simulations/"))
            assertTrue(file.fileSize > 0)
        }

        @Test
        fun `should link saved files to created simulation`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            val sim = simulationRepo.findAll().first()
            val file = fileRepo.findAll().first()

            assertEquals(sim.id, file.simulation.id)
        }
    }

    @Nested
    inner class OptionalFiles {

        @Test
        fun `should save mp file when requested`() {
            val user = createUser()
            val request = params().copy(mp = "mp.txt")

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "mp.txt").writeText("mp content")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, request)

            val files = fileRepo.findAll()
            assertTrue(files.any { it.fileType == FileType.MP && it.fileName == "mp.txt" })
        }

        @Test
        fun `should save pp file when requested`() {
            val user = createUser()
            val request = params().copy(pP = "pp.txt")

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "pp.txt").writeText("pp content")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, request)

            val files = fileRepo.findAll()
            assertTrue(files.any { it.fileType == FileType.PP && it.fileName == "pp.txt" })
        }

        @Test
        fun `should save pe file when requested`() {
            val user = createUser()
            val request = params().copy(pE = "pe.txt")

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "pe.txt").writeText("pe content")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, request)

            val files = fileRepo.findAll()
            assertTrue(files.any { it.fileType == FileType.PE && it.fileName == "pe.txt" })
        }

/*
        @Test
        fun `should save zipped output when requested`() {
            val user = createUser()
            val request = params().copy(zippedOutput = true)

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "results.zip").writeText("zip content")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, request)

            val files = fileRepo.findAll()
            assertTrue(files.any { it.fileType == FileType.ZIPPED && it.fileName == "results.zip" })
        }
 */

        @Test
        fun `should not save optional files when not requested`() {
            val user = createUser()

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                File(simulationDir, "mp.txt").writeText("mp")
                File(simulationDir, "pp.txt").writeText("pp")
                File(simulationDir, "pe.txt").writeText("pe")
                File(simulationDir, "results.zip").writeText("zip")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()
            service.runSimulation(user.username, params())

            val files = fileRepo.findAll()
            assertEquals(1, files.size)
            assertEquals(FileType.LOG, files.first().fileType)
        }

        @Test
        fun `should throw when requested mp file is missing`() {
            val user = createUser()
            val request = params().copy(mp = "mp.txt")

            every { runner.run(any(), any()) } answers {
                val simulationDir = secondArg<File>()
                File(simulationDir, "log.txt").writeText("log")
                SimulationRunResult(0, File(simulationDir, "log.txt"))
            }

            val service = buildService()

            assertThrows(IllegalArgumentException::class.java) {
                service.runSimulation(user.username, request)
            }
        }
    }
}