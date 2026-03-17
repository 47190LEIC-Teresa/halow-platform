package backend.db.repositories

import backend.db.entities.SimulationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationConfigRepositoryTest {

    @Autowired
    lateinit var simulationConfigRepository: SimulationConfigRepository

    private fun buildSimulationConfig(
        nGroups: Int = 2,
        nStations: Int = 10,
        width: Int = 100,
        height: Int = 200,
        verbosity: Int = 1,
        simLength: Long = 1000L,
        packetRate: Int = 5,
        slotLength: Long = 50L
    ): SimulationConfig {
        return SimulationConfig(
            nGroups = nGroups,
            nStations = nStations,
            width = width,
            height = height,
            verbosity = verbosity,
            simLength = simLength,
            packetRate = packetRate,
            slotLength = slotLength
        )
    }

    @Test
    fun `should save simulation config and generate id`() {
        val saved = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        assertNotNull(saved.id)
    }

    @Test
    fun `should save and retrieve simulation config`() {
        val saved = simulationConfigRepository.saveAndFlush(
            buildSimulationConfig(
                nGroups = 3,
                nStations = 15,
                width = 500,
                height = 300,
                verbosity = 2,
                simLength = 10000L,
                packetRate = 8,
                slotLength = 25L
            )
        )

        val found = simulationConfigRepository.findById(saved.id!!).orElseThrow()

        assertEquals(3, found.nGroups)
        assertEquals(15, found.nStations)
        assertEquals(500, found.width)
        assertEquals(300, found.height)
        assertEquals(2, found.verbosity)
        assertEquals(10000L, found.simLength)
        assertEquals(8, found.packetRate)
        assertEquals(25L, found.slotLength)
    }

    @Test
    fun `should update simulation config`() {
        val saved = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        val found = simulationConfigRepository.findById(saved.id!!).orElseThrow()
        found.nGroups = 4
        found.nStations = 20
        found.width = 800
        found.height = 600
        found.verbosity = 3
        found.simLength = 20000L
        found.packetRate = 12
        found.slotLength = 100L

        simulationConfigRepository.saveAndFlush(found)

        val updated = simulationConfigRepository.findById(saved.id!!).orElseThrow()

        assertEquals(4, updated.nGroups)
        assertEquals(20, updated.nStations)
        assertEquals(800, updated.width)
        assertEquals(600, updated.height)
        assertEquals(3, updated.verbosity)
        assertEquals(20000L, updated.simLength)
        assertEquals(12, updated.packetRate)
        assertEquals(100L, updated.slotLength)
    }

    @Test
    fun `should delete simulation config by id`() {
        val saved = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        simulationConfigRepository.deleteById(saved.id!!)
        simulationConfigRepository.flush()

        val found = simulationConfigRepository.findById(saved.id!!)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should return empty when simulation config does not exist`() {
        val found = simulationConfigRepository.findById(999999L)

        assertTrue(found.isEmpty)
    }
}