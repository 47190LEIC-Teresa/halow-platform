package backend.backend.service

import backend.backend.model.dto.parseSimulationMetrics
import backend.backend.simulator.ISimulationParserRunner
import backend.model.dto.SimulationMetricsResponse
import backend.model.entity.SimulationMetrics
import backend.repository.SimulationMetricsRepository
import backend.repository.SimulationRepository
import backend.service.SimulationFileService
import backend.simulator.SimParserRunner
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempFile

@Service
class SimulationMetricsService(
    private val simulationMetricsRepository: SimulationMetricsRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationFileService: SimulationFileService,
    private val simParserRunner: ISimulationParserRunner = SimParserRunner(),
) {

    private fun toSimulationMetricsResponse(metrics: SimulationMetrics): SimulationMetricsResponse {
        return SimulationMetricsResponse(
            totalPackets = metrics.totalPackets,
            packetsAborted = metrics.packetsAborted,
            packetsReachedMedium = metrics.packetsReachedMedium,
            packetsDelivered = metrics.packetsDelivered,
            deliveryRateTotal = metrics.deliveryRateTotal,
            deliveryRateMedium = metrics.deliveryRateMedium,

            dataFrameTransmissionAttempts = metrics.dataFrameTransmissionAttempts,
            dataFrameTransmissionSuccesses = metrics.dataFrameTransmissionSuccesses,
            dataFrameAckReceptions = metrics.dataFrameAckReceptions,
            frameDeliveryRateForward = metrics.frameDeliveryRateForward,
            frameDeliveryRateBackward = metrics.frameDeliveryRateBackward,
            frameDeliveryRateBidirectional = metrics.frameDeliveryRateBidirectional,

            averageDelayUs = metrics.averageDelayUs,
            delayStdDevUs = metrics.delayStdDevUs,

            framesWithoutCollision = metrics.framesWithoutCollision,
            framesReceivedWithCollision = metrics.framesReceivedWithCollision,
            framesDroppedWithCollision = metrics.framesDroppedWithCollision,
            collidedFramesFraction = metrics.collidedFramesFraction
        )
    }

    @Transactional(readOnly = true)
    fun getMetrics(simulationId: Long): ResponseEntity<SimulationMetricsResponse> {
        val metrics =  simulationMetricsRepository.findBySimulationId(simulationId)
            ?: return ResponseEntity.noContent().build()

        return ResponseEntity.ok(toSimulationMetricsResponse(metrics))
    }

    @Transactional
    fun runSimulation(simulationId: Long) : SimulationMetricsResponse {
        val simulation = simulationRepository.findById(simulationId)
            .orElseThrow { IllegalArgumentException("Simulation not found: $simulationId") }

        val logFile = simulationFileService.createTempLogFileForSimulation(simulationId)

        try {
            val result = simParserRunner.run(logFile)

            if (result.exitCode != 0) { //TODO: handle this better, maybe with a custom exception type
                throw RuntimeException("SimParser failed with exit code ${result.exitCode}")
            }

            val metrics = parseSimulationMetrics(result.rawOutput)

            val simMetrics = SimulationMetrics(
                simulation = simulation,
                totalPackets = metrics.totalPackets,
                packetsAborted = metrics.packetsAborted,
                packetsReachedMedium = metrics.packetsReachedMedium,
                packetsDelivered = metrics.packetsDelivered,
                deliveryRateTotal = metrics.deliveryRateTotal,
                deliveryRateMedium = metrics.deliveryRateMedium,

                dataFrameTransmissionAttempts = metrics.dataFrameTransmissionAttempts,
                dataFrameTransmissionSuccesses = metrics.dataFrameTransmissionSuccesses,
                dataFrameAckReceptions = metrics.dataFrameAckReceptions,
                frameDeliveryRateForward = metrics.frameDeliveryRateForward,
                frameDeliveryRateBackward = metrics.frameDeliveryRateBackward,
                frameDeliveryRateBidirectional = metrics.frameDeliveryRateBidirectional,

                averageDelayUs = metrics.averageDelayUs,
                delayStdDevUs = metrics.delayStdDevUs,

                framesWithoutCollision = metrics.framesWithoutCollision,
                framesReceivedWithCollision = metrics.framesReceivedWithCollision,
                framesDroppedWithCollision = metrics.framesDroppedWithCollision,
                collidedFramesFraction = metrics.collidedFramesFraction
            )

            simulationMetricsRepository.save(simMetrics)
            return toSimulationMetricsResponse(simMetrics)  //we could simply return `metrics` here, but this way we ensure the response is consistent with what is stored in the database

        } finally {
            if (!logFile.delete()) {
                println("Warning: failed to delete temp log file ${logFile.absolutePath}")
            }
        }
    }

    @Transactional(readOnly = true)
    fun getFileMetrics(file: MultipartFile): SimulationMetricsResponse {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty")
        }

        val originalName = file.originalFilename ?: "log.zip"
        if (!originalName.endsWith(".zip", ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .zip log files are supported")
        }

        val tempZipPath: Path = createTempFile(prefix = "metrics-upload-", suffix = ".zip")
        val tempTxtPath: Path = createTempFile(prefix = "metrics-upload-unzipped-", suffix = ".txt")


        try {
            file.transferTo(tempZipPath)

            GZIPInputStream(Files.newInputStream(tempZipPath)).use { gzipInput ->
                Files.newOutputStream(tempTxtPath).use { output ->
                    gzipInput.copyTo(output)
                }
            }

            val result = simParserRunner.run(tempTxtPath.toFile())

            if (result.exitCode != 0) {
                throw ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "SimParser failed with exit code ${result.exitCode}"
                )
            }

            return parseSimulationMetrics(result.rawOutput)

        } finally {
            try {
                Files.deleteIfExists(tempZipPath)
            } catch (_: Exception) {
            }

            try {
                Files.deleteIfExists(tempTxtPath)
            } catch (_: Exception) {
            }
        }
    }
}
