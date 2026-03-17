package backend.db.repositories

import backend.db.entities.*
import backend.db.enums.FileType
import backend.db.enums.LogStatus
import backend.db.enums.SimulationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationFileRepositoryTest {

    @Autowired
    lateinit var simulationFileRepository: SimulationFileRepository

    @Autowired
    lateinit var simulationRepository: SimulationRepository

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    @Autowired
    lateinit var simulationConfigRepository: SimulationConfigRepository

    private fun buildUser() = AppUser(
        username = "user_1",
        email = "user1@test.com",
        firstName = "User",
        lastName = "One",
        passwordHash = "hash",
        lastAccess = null
    )

    private fun buildConfig() = SimulationConfig(
        nGroups = 2,
        nStations = 10,
        width = 100,
        height = 100,
        verbosity = 1,
        simLength = 1000,
        packetRate = 5,
        slotLength = 50
    )

    private fun buildSimulation(user: AppUser, config: SimulationConfig) = Simulation(
        user = user,
        config = config,
        status = SimulationStatus.QUEUED,
        logStatus = LogStatus.NOT_READY,
        seed = 123,
        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0)
    )

    private fun buildFile(simulation: Simulation) = SimulationFile(
        simulation = simulation,
        fileType = FileType.LOG,
        fileName = "log.txt",
        fileUrl = "http://test/log.txt",
        fileSize = 100L
    )

    private fun saveSimulation(): Simulation {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildConfig())
        return simulationRepository.saveAndFlush(buildSimulation(user, config))
    }

    @Test
    fun `should save simulation file and generate id`() {
        val sim = saveSimulation()

        val saved = simulationFileRepository.saveAndFlush(buildFile(sim))

        assertNotNull(saved.id)
    }

    @Test
    fun `should persist simulation file fields and relation`() {
        val sim = saveSimulation()

        val saved = simulationFileRepository.saveAndFlush(
            SimulationFile(
                simulation = sim,
                fileType = FileType.LOG,
                fileName = "log.txt",
                fileUrl = "http://test/log.txt",
                fileSize = 500L
            )
        )

        val found = simulationFileRepository.findById(saved.id!!).orElseThrow()

        assertEquals(FileType.LOG, found.fileType)
        assertEquals("log.txt", found.fileName)
        assertEquals("http://test/log.txt", found.fileUrl)
        assertEquals(500L, found.fileSize)
        assertEquals(sim.id, found.simulation.id)
    }

    @Test
    fun `should update simulation file`() {
        val sim = saveSimulation()
        val saved = simulationFileRepository.saveAndFlush(buildFile(sim))

        val found = simulationFileRepository.findById(saved.id!!).orElseThrow()
        found.fileName = "updated.txt"
        found.fileUrl = "http://test/updated.txt"
        found.fileSize = 999L

        simulationFileRepository.saveAndFlush(found)

        val updated = simulationFileRepository.findById(saved.id!!).orElseThrow()

        assertEquals("updated.txt", updated.fileName)
        assertEquals("http://test/updated.txt", updated.fileUrl)
        assertEquals(999L, updated.fileSize)
    }

    @Test
    fun `should delete simulation file by id`() {
        val sim = saveSimulation()
        val saved = simulationFileRepository.saveAndFlush(buildFile(sim))

        simulationFileRepository.deleteById(saved.id!!)
        simulationFileRepository.flush()

        val found = simulationFileRepository.findById(saved.id!!)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should return empty when simulation file does not exist`() {
        val found = simulationFileRepository.findById(999999L)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should persist simulation relation`() {
        val sim = saveSimulation()

        val saved = simulationFileRepository.saveAndFlush(buildFile(sim))

        val found = simulationFileRepository.findById(saved.id!!).orElseThrow()

        assertEquals(sim.id, found.simulation.id)
    }
}