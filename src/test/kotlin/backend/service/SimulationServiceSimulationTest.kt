package backend.service

import backend.exception.SimulationAccessDeniedException
import backend.model.dto.CreateSimulationBatchRequest
import backend.model.enums.SimulationStatus
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SimulationServiceSimulationTest : BaseSimulationServiceJpaTest() {

    @Test
    fun `submitSimulation should create simulation and job`() {
        val user = createUser()
        val service = buildService()

        assertEquals(0, simulationRepo.count())

        val response = service.submitSimulation(
            username = user.username,
            params = params(),
            runSimParser = true,
            gFile = null,
            label = "Test sim"
        )

        assertEquals(1, simulationRepo.count())

        val sim = simulationRepo.findById(response.simulationId).orElseThrow()

        assertEquals(user.username, sim.user.username)
        assertEquals(SimulationStatus.CREATED, sim.status)
        assertEquals("Test sim", sim.label)
        assertEquals(params().seed, sim.seed)
        assertEquals(true, sim.wMetrics)
        assertEquals(params().zippedOutput, sim.zippedOutput)

        verify(exactly = 1) {
            jobService.createJob(simulation = sim, gFileId = any())
        }
    }

    @Test
    fun `rerunSimulation should create new simulation with parent id`() {
        val owner = createUser()
        val service = buildService()

        val first = service.submitSimulation(
            username = owner.username,
            params = params(),
            runSimParser = true,
            gFile = null,
            label = "Original"
        )

        val original = simulationRepo.findById(first.simulationId).orElseThrow()

        val rerunResponse = service.rerunSimulation(
            simulationID = original.id!!,
            username = owner.username
        )

        assertEquals(2, simulationRepo.count())

        val rerun = simulationRepo.findById(rerunResponse.simulationId).orElseThrow()

        assertEquals(owner.username, rerun.user.username)
        assertEquals(SimulationStatus.CREATED, rerun.status)
        assertEquals(original.id, rerun.parentSimulationId)
    }

    @Test
    fun `rerunSimulation should throw SimulationAccessDeniedException when user does not own simulation`() {
        val owner = createUser()
        val other = createUser(
            username = "other_user",
            email = "other@test.com"
        )

        val service = buildService()

        val first = service.submitSimulation(
            username = owner.username,
            params = params(),
            runSimParser = true,
            gFile = null,
            label = "Original"
        )

        val original = simulationRepo.findById(first.simulationId).orElseThrow()

        assertThrows(SimulationAccessDeniedException::class.java) {
            service.rerunSimulation(
                simulationID = original.id!!,
                username = other.username
            )
        }
    }

    @Test
    fun `submitSimulationBatch should create batchSize simulations with sequential seeds`() {
        val user = createUser()
        val service = buildService()

        val request = CreateSimulationBatchRequest(
            batchSize = 3,
            n = 10,
            g = 2,
            h = 100,
            w = 100,
            seedMin = 1,
            seedMax = 3,
            randomSeed = false,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 5,
            slotLength = 50L,
            zippedOutput = false,
            pE = null,
            pP = null,
            mp = null,
            runSimParser = true,
            label = "Batch run"
        )

        val responses = service.submitSimulationBatch(
            username = user.username,
            request = request,
            gFile = null
        )

        assertEquals(3, simulationRepo.count())
        requireNotNull(responses)
        assertEquals(3, responses.size)

        val savedSeeds = simulationRepo.findAll().map { it.seed }.sorted()
        assertEquals(listOf(1, 2, 3), savedSeeds)
    }
}