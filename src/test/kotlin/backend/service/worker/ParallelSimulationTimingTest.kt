package backend.service.worker

import backend.model.dto.CreateSimulationBatchRequest
import backend.model.enums.SimulationStatus
import backend.service.BaseSimulationServiceJpaTest
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParallelSimulationTimingTest : BaseSimulationServiceJpaTest() {

    @Test
    fun `parallel batch with increased load should complete and report total time`() {
        val user = createUser(
            username = "parallel_user",
            email = "parallel_user@test.com"
        )
        val service = buildService()

        val batchRequest = CreateSimulationBatchRequest(
            batchSize = 8,
            n = 10,
            g = 2,
            h = 100,
            w = 100,
            seedMin = 1,
            seedMax = 8,
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
            label = "Parallel batch timing test"
        )

        val startedAt = System.nanoTime()

        val responses = service.submitSimulationBatch(
            username = user.username,
            request = batchRequest,
            gFile = null
        )

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000

        println("submitSimulationBatch elapsedMs=$elapsedMs")
        println("responses size=${responses?.size}")

        val sims = simulationRepo.findAll()
            .filter { it.user.username == user.username }
            .sortedBy { it.seed }

        println("persisted sims size=${sims.size}")
        sims.forEachIndexed { index, sim ->
            println(
                "sim[$index]: id=${sim.id}, seed=${sim.seed}, status=${sim.status}, " +
                        "label=${sim.label}, wMetrics=${sim.wMetrics}, zippedOutput=${sim.zippedOutput}"
            )
        }

        assertEquals(batchRequest.batchSize, responses?.size)
        assertEquals(batchRequest.batchSize, sims.size)

        val expectedSeeds = (batchRequest.seedMin!!..batchRequest.seedMax!!).toList()
        assertEquals(expectedSeeds, sims.map { it.seed })

        assertTrue(sims.all { it.status == SimulationStatus.CREATED })
        assertTrue(sims.all { it.label == batchRequest.label })
        assertTrue(sims.all { it.wMetrics == batchRequest.runSimParser })
        assertTrue(sims.all { it.zippedOutput == batchRequest.zippedOutput })

        verify(exactly = batchRequest.batchSize) {
            jobService.createJob(simulation = any(), gFileId = any())
        }

        println("average submit time per simulation=${elapsedMs.toDouble() / batchRequest.batchSize} ms")
    }
}