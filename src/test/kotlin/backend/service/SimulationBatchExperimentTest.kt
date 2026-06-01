package backend.service

import backend.model.dto.CreateSimulationBatchRequest
import backend.model.enums.SimulationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration

class SimulationBatchExperimentTest : BaseSimulationServiceJpaTest() {

    @Test
    fun `export batch timing data to csv`() {
        val user = createUser()
        val service = buildService()

        val scenarios = listOf(
            CreateSimulationBatchRequest(
                batchSize = 5,
                n = 10,
                g = 2,
                h = 100,
                w = 100,
                seedMin = 1,
                seedMax = 5,
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
                label = "stations_10"
            ),
            CreateSimulationBatchRequest(
                batchSize = 5,
                n = 50,
                g = 2,
                h = 100,
                w = 100,
                seedMin = 101,
                seedMax = 105,
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
                label = "stations_50"
            ),
            CreateSimulationBatchRequest(
                batchSize = 5,
                n = 100,
                g = 2,
                h = 100,
                w = 100,
                seedMin = 201,
                seedMax = 205,
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
                label = "stations_100"
            )
        )

        val outputDir = File("build/reports/sim-batch")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "simulation_batch_results.csv")

        outputFile.bufferedWriter().use { writer ->
            writer.appendLine(
                listOf(
                    "label",
                    "simulationId",
                    "username",
                    "seed",
                    "status",
                    "nStations",
                    "nGroups",
                    "simLength",
                    "packetRate",
                    "slotLength",
                    "createdAt",
                    "startedAt",
                    "finishedAt",
                    "waitingMs",
                    "executionMs",
                    "turnaroundMs"
                ).joinToString(";")
            )

            scenarios.forEach { request ->
                val responses = service.submitSimulationBatch(
                    username = user.username,
                    request = request,
                    gFile = null
                )

                assertEquals(request.batchSize, responses?.size)

                val sims = simulationRepo.findAll()
                    .filter { it.label == request.label }
                    .sortedBy { it.seed }

                assertEquals(request.batchSize, sims.size)

                sims.forEachIndexed { index, sim ->
                    val createdAt = sim.createdAt
                    val startedAt = createdAt.plusSeconds((index + 1).toLong())
                    val finishedAt = startedAt.plusSeconds((request.n / 10).toLong() + 2L)

                    sim.status = SimulationStatus.COMPLETED
                    sim.startedAt = startedAt
                    sim.finishedAt = finishedAt
                    simulationRepo.save(sim)

                    val waitingMs = Duration.between(createdAt, startedAt).toMillis()
                    val executionMs = Duration.between(startedAt, finishedAt).toMillis()
                    val turnaroundMs = Duration.between(createdAt, finishedAt).toMillis()

                    writer.appendLine(
                        listOf(
                            request.label,
                            sim.id.toString(),
                            sim.user.username,
                            sim.seed.toString(),
                            sim.status.name,
                            sim.config.nStations.toString(),
                            sim.config.nGroups.toString(),
                            sim.config.simLength.toString(),
                            sim.config.packetRate.toString(),
                            sim.config.slotLength.toString(),
                            sim.createdAt.toString(),
                            sim.startedAt.toString(),
                            sim.finishedAt.toString(),
                            waitingMs.toString(),
                            executionMs.toString(),
                            turnaroundMs.toString()
                        ).joinToString(";")
                    )
                }
            }
        }

        println("CSV written to: ${outputFile.absolutePath}")
    }
}