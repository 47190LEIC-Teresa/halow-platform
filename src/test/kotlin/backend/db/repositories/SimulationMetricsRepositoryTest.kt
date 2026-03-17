package backend.db.repositories

import backend.db.entities.AppUser
import backend.db.entities.Simulation
import backend.db.entities.SimulationConfig
import backend.db.entities.SimulationMetrics
import backend.db.enums.LogStatus
import backend.db.enums.SimulationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationMetricsRepositoryTest {

    @Autowired
    lateinit var simulationMetricsRepository: SimulationMetricsRepository

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

    private fun buildMetrics(simulation: Simulation) = SimulationMetrics(
        simulation = simulation,
        totalPackets = 1000,
        packetsAborted = 100,
        packetsReachedMedium = 800,
        deliveryRateTotal = 0.70,
        deliveryRateMedium = 0.875
    )

    private fun saveSimulation(): Simulation {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildConfig())
        return simulationRepository.saveAndFlush(buildSimulation(user, config))
    }

    @Test
    fun `should save simulation metrics and generate id`() {
        val simulation = saveSimulation()

        val saved = simulationMetricsRepository.saveAndFlush(buildMetrics(simulation))

        assertNotNull(saved.id)
    }

    @Test
    fun `should persist simulation metrics fields and relation`() {
        val simulation = saveSimulation()

        val saved = simulationMetricsRepository.saveAndFlush(
            SimulationMetrics(
                simulation = simulation,
                totalPackets = 5000,
                packetsAborted = 250,
                packetsReachedMedium = 4200,
                deliveryRateTotal = 0.84,
                deliveryRateMedium = 0.91
            )
        )

        val found = simulationMetricsRepository.findById(saved.id!!).orElseThrow()

        assertEquals(simulation.id, found.simulation.id)
        assertEquals(5000, found.totalPackets)
        assertEquals(250, found.packetsAborted)
        assertEquals(4200, found.packetsReachedMedium)
        assertEquals(0.84, found.deliveryRateTotal)
        assertEquals(0.91, found.deliveryRateMedium)
    }

    @Test
    fun `should update simulation metrics`() {
        val simulation = saveSimulation()
        val saved = simulationMetricsRepository.saveAndFlush(buildMetrics(simulation))

        val found = simulationMetricsRepository.findById(saved.id!!).orElseThrow()
        found.totalPackets = 2000
        found.packetsAborted = 150
        found.packetsReachedMedium = 1700
        found.deliveryRateTotal = 0.85
        found.deliveryRateMedium = 0.94

        simulationMetricsRepository.saveAndFlush(found)

        val updated = simulationMetricsRepository.findById(saved.id!!).orElseThrow()

        assertEquals(2000, updated.totalPackets)
        assertEquals(150, updated.packetsAborted)
        assertEquals(1700, updated.packetsReachedMedium)
        assertEquals(0.85, updated.deliveryRateTotal)
        assertEquals(0.94, updated.deliveryRateMedium)
    }

    @Test
    fun `should delete simulation metrics by id`() {
        val simulation = saveSimulation()
        val saved = simulationMetricsRepository.saveAndFlush(buildMetrics(simulation))

        simulationMetricsRepository.deleteById(saved.id!!)
        simulationMetricsRepository.flush()

        val found = simulationMetricsRepository.findById(saved.id!!)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should return empty when simulation metrics does not exist`() {
        val found = simulationMetricsRepository.findById(999999L)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should fail when saving second metrics row for same simulation`() {
        val simulation = saveSimulation()
        simulationMetricsRepository.saveAndFlush(buildMetrics(simulation))

        assertThrows<DataIntegrityViolationException> {
            simulationMetricsRepository.saveAndFlush(
                SimulationMetrics(
                    simulation = simulation,
                    totalPackets = 1,
                    packetsAborted = 1,
                    packetsReachedMedium = 1,
                    deliveryRateTotal = 0.1,
                    deliveryRateMedium = 0.1
                )
            )
        }
    }
}