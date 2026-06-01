package backend.service

import backend.exception.EmptyUploadException
import backend.exception.InvalidMetricsFileTypeException
import backend.exception.SimParserFailedException
import backend.exception.SimulationMetricsNotFoundException
import backend.exception.SimulationNotFoundException
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.entity.SimulationMetrics
import backend.model.entity.User
import backend.model.enums.LogStatus
import backend.model.enums.MetricsStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationMetricsRepository
import backend.repository.SimulationRepository
import backend.simulator.SimParserRunner
import backend.simulator.model.SimulationParseRunResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import java.util.Optional

class SimulationMetricsServiceTest {

    private lateinit var simulationMetricsRepository: SimulationMetricsRepository
    private lateinit var simulationRepository: SimulationRepository
    private lateinit var simulationFileService: SimulationFileService
    private lateinit var simParserRunner: SimParserRunner
    private lateinit var service: SimulationMetricsService

    @BeforeEach
    fun setUp() {
        simulationMetricsRepository = mock()
        simulationRepository = mock()
        simulationFileService = mock()
        simParserRunner = mock()

        service = SimulationMetricsService(
            simulationMetricsRepository = simulationMetricsRepository,
            simulationRepository = simulationRepository,
            simulationFileService = simulationFileService,
            simParserRunner = simParserRunner
        )
    }

    @Test
    fun `getMetrics should return mapped response`() {
        val simulation = buildSimulation()
        val metrics = SimulationMetrics(
            id = 1L,
            simulation = simulation,
            totalPackets = 100,
            packetsAborted = 10,
            packetsReachedMedium = 90,
            packetsDelivered = 80,
            deliveryRateTotal = 0.8,
            deliveryRateMedium = 0.888,
            dataFrameTransmissionAttempts = 50,
            dataFrameTransmissionSuccesses = 45,
            dataFrameAckReceptions = 44,
            frameDeliveryRateForward = 0.9,
            frameDeliveryRateBackward = 0.88,
            frameDeliveryRateBidirectional = 0.89,
            averageDelayUs = 123.0,
            delayStdDevUs = 12.0,
            framesWithoutCollision = 70,
            framesReceivedWithCollision = 5,
            framesDroppedWithCollision = 3,
            collidedFramesFraction = 0.08
        )

        whenever(simulationMetricsRepository.findBySimulationId(1L)).thenReturn(metrics)

        val response = service.getMetrics(1L)

        assertEquals(100, response.totalPackets)
        assertEquals(80, response.packetsDelivered)
    }

    @Test
    fun `getMetrics should throw when missing`() {
        whenever(simulationMetricsRepository.findBySimulationId(1L)).thenReturn(null)

        assertThrows(SimulationMetricsNotFoundException::class.java) {
            service.getMetrics(1L)
        }
    }

    @Test
    fun `runSimulation should complete successfully`() {
        val simulation = buildSimulation()
        val tempLog = kotlin.io.path.createTempFile("metrics-log", ".txt").toFile().apply {
            writeText("log")
        }

        whenever(simulationRepository.findById(1L)).thenReturn(Optional.of(simulation))
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationFileService.createTempLogFileForSimulation(1L)).thenReturn(tempLog)
        whenever(simParserRunner.run(tempLog)).thenReturn(
            SimulationParseRunResult(
                exitCode = 0,
                rawOutput = """
            Total number of application layer packets: 100
            Number of packets that were eventually aborted: 10
            Number of packets that eventually reached the medium access phase (after successful association) (including retransmissions): 90
            Number of packets that were actually delivered at the receiver's application layer: 80
            Packet delivery rate wrt total number of generated packets: 0.8
            Packet delivery rate wrt those that actually reached medium access: 0.888
            Data frame transmission attempts: 50
            Data frame transmission successes (only forward direction): 45
            Data frame ack receptions (complete success): 44
            Frame delivery rate (forward): 0.9
            Frame delivery rate (backward): 0.88
            Frame delivery rate (bidirectional): 0.89
            Average: 123.0 us
            Standard deviation: 12.0 us
            Number of data frames received withOUT collision: 70
            Number of data frames received even with collision: 5
            Number of data frames dropped with collision: 3
            Total fraction of collided data frames: 0.08
        """.trimIndent()
            )
        )
        whenever(simulationMetricsRepository.save(any())).thenAnswer { it.arguments[0] as SimulationMetrics }

        val response = service.runSimulation(1L)

        assertEquals(MetricsStatus.COMPLETED, simulation.metricsStatus)
        assertEquals(null, simulation.metricsErrorMsg)
        assertEquals(100, response.totalPackets)
    }

    @Test
    fun `runSimulation should mark failed when parser returns non zero`() {
        val simulation = buildSimulation()
        val tempLog = File.createTempFile("metrics-fail", ".txt")

        whenever(simulationRepository.findById(1L)).thenReturn(Optional.of(simulation))
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationFileService.createTempLogFileForSimulation(1L)).thenReturn(tempLog)
        whenever(simParserRunner.run(tempLog)).thenReturn(
            SimulationParseRunResult(exitCode = 2, rawOutput = "")
        )

        assertThrows(SimParserFailedException::class.java) {
            service.runSimulation(1L)
        }

        assertEquals(MetricsStatus.FAILED, simulation.metricsStatus)
    }

    @Test
    fun `runSimulation should mark failed when parser throws`() {
        val simulation = buildSimulation()
        val tempLog = File.createTempFile("metrics-throw", ".txt")

        whenever(simulationRepository.findById(1L)).thenReturn(Optional.of(simulation))
        whenever(simulationRepository.save(any())).thenAnswer { it.arguments[0] as Simulation }
        whenever(simulationFileService.createTempLogFileForSimulation(1L)).thenReturn(tempLog)
        whenever(simParserRunner.run(tempLog)).thenThrow(RuntimeException("parser crashed"))

        assertThrows(RuntimeException::class.java) {
            service.runSimulation(1L)
        }

        assertEquals(MetricsStatus.FAILED, simulation.metricsStatus)
        assertEquals("parser crashed", simulation.metricsErrorMsg)
    }

    @Test
    fun `runSimulation should throw when simulation missing`() {
        whenever(simulationRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows(SimulationNotFoundException::class.java) {
            service.runSimulation(1L)
        }
    }

    @Test
    fun `getFileMetrics should throw for empty file`() {
        val file = MockMultipartFile("file", "log.zip", "application/zip", byteArrayOf())

        assertThrows(EmptyUploadException::class.java) {
            service.getFileMetrics(file)
        }
    }

    @Test
    fun `getFileMetrics should throw for invalid extension`() {
        val file = MockMultipartFile("file", "log.txt", "text/plain", "abc".toByteArray())

        assertThrows(InvalidMetricsFileTypeException::class.java) {
            service.getFileMetrics(file)
        }
    }

    private fun buildSimulation(): Simulation {
        val user = User(
            id = 1L,
            username = "john",
            passwordHash = "hash",
            email = "john@test.com",
            firstName = "John",
            lastName = "Doe",
            lastAccess = null
        )

        val config = SimulationConfig(
            id = 1L,
            nGroups = 2,
            nStations = 10,
            width = 100,
            height = 100,
            verbosity = 1,
            simLength = 1000L,
            packetRate = 5,
            slotLength = 50L
        )

        return Simulation(
            id = 1L,
            user = user,
            config = config,
            status = SimulationStatus.COMPLETED,
            logStatus = LogStatus.READY,
            label = "sim",
            errorMsg = null,
            createdAt = java.time.LocalDateTime.now(),
            startedAt = null,
            finishedAt = null,
            seed = 123,
            wPe = false,
            wPp = false,
            wMp = false,
            peName = null,
            ppName = null,
            mpName = null,
            wGroupFile = false,
            wMetrics = true,
            zippedOutput = false,
            parentSimulationId = null,
            metricsStatus = MetricsStatus.NOT_REQUESTED,
            metricsErrorMsg = null
        )
    }
}