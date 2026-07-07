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
    fun `parallel batch submission with default style config should report timing`() {
        val user = createUser(
            username = "parallel_user",
            email = "parallel_user@test.com"
        )
        val service = buildService()

        val batchRequest = CreateSimulationBatchRequest(
            batchSize = 4,
            n = 10,
            g = 2,
            h = 100,
            w = 100,
            seedMin = 1,
            seedMax = 4,
            randomSeed = false,
            verbosity = 1,
            simLength = 100L,
            packetRate = 1,
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

        val sims = simulationRepo.findAll()
            .filter { it.user.username == user.username }
            .sortedBy { it.seed }

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

        println(
            "BATCH_RESULT," +
                    "batchSize=${batchRequest.batchSize}," +
                    "stations=${batchRequest.n}," +
                    "groups=${batchRequest.g}," +
                    "simLength=${batchRequest.simLength}," +
                    "packetRate=${batchRequest.packetRate}," +
                    "submitElapsedMs=$elapsedMs," +
                    "avgSubmitMs=${elapsedMs.toDouble() / batchRequest.batchSize}"
        )
    }
}