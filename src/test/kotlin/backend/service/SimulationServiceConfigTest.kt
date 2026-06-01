package backend.service

import backend.model.entity.SimulationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class SimulationServiceConfigTest : BaseSimulationServiceJpaTest() {

    @Test
    fun `should reuse existing config`() {
        val user = createUser()

        val existing = configRepo.saveAndFlush(
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

        val service = buildService()

        val response = service.submitSimulation(
            username = user.username,
            params = params(),
            runSimParser = false,
            gFile = null,
            label = null
        )

        assertEquals(1, configRepo.count())

        val sim = simulationRepo.findById(response.simulationId).orElseThrow()
        assertEquals(existing.id, sim.config.id)
    }

    @Test
    fun `should create config when no matching config exists`() {
        val user = createUser()
        val service = buildService()

        assertEquals(0, configRepo.count())

        val response = service.submitSimulation(
            username = user.username,
            params = params(),
            runSimParser = false,
            gFile = null,
            label = null
        )

        assertEquals(1, configRepo.count())

        val sim = simulationRepo.findById(response.simulationId).orElseThrow()
        assertNotEquals(null, sim.config.id)
        assertEquals(2, sim.config.nGroups)
        assertEquals(10, sim.config.nStations)
        assertEquals(100, sim.config.width)
        assertEquals(100, sim.config.height)
    }
}