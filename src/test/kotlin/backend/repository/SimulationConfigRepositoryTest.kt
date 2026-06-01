package backend.repository

import backend.model.entity.SimulationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `findMatchingConfig returns config when all parameters match`() {
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

        val found = simulationConfigRepository.findMatchingConfig(
            nGroups = 3,
            nStations = 15,
            width = 500,
            height = 300,
            verbosity = 2,
            simLength = 10000L,
            packetRate = 8,
            slotLength = 25L
        )

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
    }

    @Test
    fun `findMatchingConfig returns null when one parameter differs`() {
        simulationConfigRepository.saveAndFlush(
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

        val found = simulationConfigRepository.findMatchingConfig(
            nGroups = 3,
            nStations = 15,
            width = 500,
            height = 300,
            verbosity = 2,
            simLength = 10000L,
            packetRate = 9,
            slotLength = 25L
        )

        assertNull(found)
    }

    @Test
    fun `findMatchingConfig returns correct config when multiple configs exist`() {
        val expected = simulationConfigRepository.saveAndFlush(
            buildSimulationConfig(
                nGroups = 2,
                nStations = 10,
                width = 100,
                height = 200,
                verbosity = 1,
                simLength = 1000L,
                packetRate = 5,
                slotLength = 50L
            )
        )

        simulationConfigRepository.saveAndFlush(
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

        val found = simulationConfigRepository.findMatchingConfig(
            nGroups = 2,
            nStations = 10,
            width = 100,
            height = 200,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 5,
            slotLength = 50L
        )

        assertNotNull(found)
        assertEquals(expected.id, found!!.id)
    }
}