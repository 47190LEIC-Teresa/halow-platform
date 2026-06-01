package backend.service

import backend.exception.EmptyUploadException
import backend.exception.InvalidMetricsFileTypeException
import backend.exception.SimParserFailedException
import backend.exception.SimulationMetricsNotFoundException
import backend.exception.SimulationNotFoundException
import backend.model.dto.parseSimulationMetrics
import backend.model.dto.SimulationMetricsResponse
import backend.model.entity.SimulationMetrics
import backend.model.enums.MetricsStatus
import backend.repository.SimulationMetricsRepository
import backend.repository.SimulationRepository
import backend.simulator.SimParserRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempFile

@Service
class SimulationMetricsService(
    private val simulationMetricsRepository: SimulationMetricsRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationFileService: SimulationFileService,
    private val simParserRunner: SimParserRunner,
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
    fun getMetrics(simulationId: Long): SimulationMetricsResponse {
        val metrics = simulationMetricsRepository.findBySimulationId(simulationId)
            ?: throw SimulationMetricsNotFoundException(simulationId)
        return toSimulationMetricsResponse(metrics)
    }

    @Transactional
    fun runSimulation(simulationId: Long): SimulationMetricsResponse {
        val simulation = simulationRepository.findById(simulationId)
            .orElseThrow { SimulationNotFoundException(simulationId) }

        simulation.metricsStatus = MetricsStatus.PENDING
        simulation.metricsErrorMsg = null
        simulationRepository.save(simulation)

        val logFile = simulationFileService.createTempLogFileForSimulation(simulationId)

        try {
            val result = simParserRunner.run(logFile)

            if (result.exitCode != 0) {
                throw SimParserFailedException(result.exitCode)
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

            simulation.metricsStatus = MetricsStatus.COMPLETED
            simulation.metricsErrorMsg = null
            simulationRepository.save(simulation)

            return toSimulationMetricsResponse(simMetrics)

        } catch (e: Exception) {
            simulation.metricsStatus = MetricsStatus.FAILED
            simulation.metricsErrorMsg = e.message ?: e::class.simpleName ?: "Unknown metrics error"
            simulationRepository.save(simulation)
            throw e
        } finally {
            if (!logFile.delete()) {
                println("Warning: failed to delete temp log file ${logFile.absolutePath}")
            }
        }
    }

    @Transactional(readOnly = true)
    fun getFileMetrics(file: MultipartFile): SimulationMetricsResponse {
        if (file.isEmpty) {
            throw EmptyUploadException()
        }

        val originalName = file.originalFilename ?: "log.zip"
        if (!originalName.endsWith(".zip", ignoreCase = true)) {
            throw InvalidMetricsFileTypeException(originalName)
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
                throw SimParserFailedException(result.exitCode)
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
