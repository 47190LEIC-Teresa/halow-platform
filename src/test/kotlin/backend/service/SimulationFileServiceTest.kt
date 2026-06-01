package backend.service

import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.User
import backend.model.enums.FileType
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import backend.repository.SimulationConfigRepository
import backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationFileServiceTest {

    @Autowired
    lateinit var userRepo: UserRepository

    @Autowired
    lateinit var configRepo: SimulationConfigRepository

    @Autowired
    lateinit var simulationRepo: SimulationRepository

    @Autowired
    lateinit var fileRepo: SimulationFileRepository

    private fun createSimulation(): Simulation {
        val user = userRepo.saveAndFlush(
            User(
                username = "user_1",
                email = "user@test.com",
                firstName = "User",
                lastName = "One",
                passwordHash = "hash"
            )
        )

        val config = configRepo.saveAndFlush(
            SimulationConfig(
                nGroups = 2,
                nStations = 10,
                width = 100,
                height = 100,
                verbosity = 1,
                simLength = 1000L,
                packetRate = 5,
                slotLength = 50L
            )
        )

        return simulationRepo.saveAndFlush(
            Simulation(
                user = user,
                config = config,
                seed = 123
            )
        )
    }

    @Test
    fun `saveFile should store log file metadata and link to simulation`() {
        val simulation = createSimulation()
        val service = SimulationFileService(fileRepo)

        val tempDir = File("build/test-files").apply { mkdirs() }
        val logFile = File(tempDir, "log.txt").apply {
            writeText("simulation log")
        }

        val saved = service.saveFile(
            simulation = simulation,
            fileType = FileType.LOG,
            file = logFile,
            filename = "log.txt"
        )

        assertEquals(FileType.LOG, saved.fileType)
        assertEquals("log.txt", saved.fileName)
        assertTrue(saved.fileSize > 0)
        assertEquals(simulation.id, saved.simulation.id)
    }
}