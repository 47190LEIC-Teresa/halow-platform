package backend.service

import backend.model.entity.SimulationConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SimulationServiceConfigTest : BaseSimulationServiceTest() {

    @Nested
    inner class ConfigResolution {

        @Test
        fun `should reuse existing config`() {
            val user = createUser()

            // Existing config matching params()
            configRepo.saveAndFlush(
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

            service.submitSimulation(
                username = user.username,
                params = params(),
                runSimParser = false,
                gFile = null,
                label = null
            )

            // No new config should have been created
            assertEquals(1, configRepo.count())
        }

        @Test
        fun `should create config when no matching config exists`() {
            val user = createUser()
            val service = buildService()

            assertEquals(0, configRepo.count())

            service.submitSimulation(
                username = user.username,
                params = params(),
                runSimParser = false,
                gFile = null,
                label = null
            )

            assertEquals(1, configRepo.count())
        }
    }
}