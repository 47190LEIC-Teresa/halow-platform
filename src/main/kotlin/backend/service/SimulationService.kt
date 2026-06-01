package backend.service

import backend.backend.model.dto.CreateSimulationBatchRequest
import backend.backend.model.dto.SimulationConfigResponse
import backend.model.dto.SimulationResponse
import backend.backend.model.dto.toConfigResponse
import backend.common.exception.ForbiddenException
import backend.common.exception.NotFoundException
import backend.model.entity.Simulation
import backend.model.entity.SimulationConfig
import backend.model.enums.FileType
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.UserRepository
import backend.repository.SimulationConfigRepository
import backend.repository.SimulationRepository
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulatorParams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class SimulationService(
    private val userRepository: UserRepository,
    private val simulationConfigRepository: SimulationConfigRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationFileService: SimulationFileService,
    private val simulationRunner: ISimulationRunner,
    private val simulationJobService: SimulationJobService
) {
    private val dateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun formatDateTime(value: LocalDateTime?): String {
        if (value == null) return "-"

        return value.format(dateTimeFormatter)
    }

    private fun toSimulationResponse(sim: Simulation): SimulationResponse {
        return SimulationResponse(
            simulationId = sim.id!!, //Id generated at DB can never be null
            status = sim.status,
            logStatus = sim.logStatus,
            label = sim.label,
            owner = sim.user.username,
            createdAt = formatDateTime(sim.createdAt),
            startedAt = formatDateTime(sim.startedAt),
            finishedAt = formatDateTime(sim.finishedAt),
            parentSimulationId = sim.parentSimulationId
        )
    }

    @Transactional
    fun submitSimulation(
        username: String,
        params: SimulatorParams,
        runSimParser: Boolean,
        gFile: File? = null,
        label: String? = null,
        parentSimulationId: Long? = null
    ): SimulationResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")

        val wGroupFile = gFile != null

        val existingConfig = simulationConfigRepository.findMatchingConfig(
            params.g,
            params.n,
            params.w,
            params.h,
            params.verbosity,
            params.simLength,
            params.packetRate,
            params.slotLength
        )

        // check if a matching config exists; if not, create a new one
        val config = existingConfig ?: simulationConfigRepository.save(
            SimulationConfig(
                nGroups = params.g,
                nStations = params.n,
                width = params.w,
                height = params.h,
                verbosity = params.verbosity,
                simLength = params.simLength,
                packetRate = params.packetRate,
                slotLength = params.slotLength
            )
        )

        val parentSim =  if (parentSimulationId != null) simulationRepository.findById(parentSimulationId)
                .orElseThrow { IllegalArgumentException("Parent simulation not found: $parentSimulationId") }
            else null

        val wGFile = wGroupFile || (parentSim != null && parentSim.wGroupFile)

        val simulation = simulationRepository.save(
            Simulation(
                user = user,
                config = config,
                status = SimulationStatus.CREATED,
                logStatus = LogStatus.NOT_READY,
                createdAt = LocalDateTime.now(),
                seed = params.seed,
                wMp = !params.mp.isNullOrBlank(),
                mpName = params.mp,
                wPp = !params.pP.isNullOrBlank(),
                ppName = params.pP,
                wPe = !params.pE.isNullOrBlank(),
                peName = params.pE,
                wGroupFile = wGFile,
                zippedOutput = params.zippedOutput,
                label = label,
                wMetrics = runSimParser,
                parentSimulationId = parentSimulationId
            )
        )

        val simulationGFile = when {
            !wGroupFile -> null

            parentSimulationId != null ->
                simulationFileService.getGroupsFile(parentSimulationId)

            else ->
                simulationFileService.saveFile(simulation, FileType.G, gFile!!, "groups_file")

        }

        simulationJobService.createJob(simulation, simulationGFile?.id)

        return SimulationResponse(
            simulationId = requireNotNull(simulation.id) { "Simulation ID was not generated" },
            status = simulation.status,
            logStatus = simulation.logStatus,
            label = simulation.label,
            createdAt = formatDateTime(simulation.createdAt),
            startedAt = null,
            finishedAt = null,
            owner = simulation.user.username
        )
    }

    fun submitSimulationBatch(
        username: String,
        request: CreateSimulationBatchRequest,
        gFile: File?
    ): List<SimulationResponse>? {

        validateBatchRequest(request)

        val seeds =
            if (request.randomSeed) {
                (1..request.batchSize).map {
                    (request.seedMin..request.seedMax).random()
                }
            } else {
                // sequential seeds from seedMin, count = batchSize
                (0 until request.batchSize).map { offset ->
                    request.seedMin + offset
                }
            }

        return seeds.map { seed ->
            submitSimulation(
                username = username,
                params = request.toSimulatorParams(seed),
                runSimParser = request.runSimParser,
                gFile = gFile,
                label = request.label
            )
        }
    }

    @Transactional(readOnly = true)
    fun getAllSimulations(): List<SimulationResponse> {
        return simulationRepository.findAll().map { sim ->
            toSimulationResponse(sim)
        }
    }

    @Transactional(readOnly = true)
    fun getSimulationById(id: Long): SimulationResponse {
        val simulation = simulationRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Simulation not found: $id") }

        return toSimulationResponse(simulation)
    }

    @Transactional(readOnly = true)
    fun getSimulationConfigBySimulationId(simulationID: Long): SimulationConfigResponse {
        val simulation = simulationRepository.findById(simulationID)
            .orElseThrow { IllegalArgumentException("Simulation not found: $simulationID") }

        return toConfigResponse(simulation)
    }

    fun rerunSimulation(simulationId: Long, username: String): SimulationResponse {
        val existing = simulationRepository.findById(simulationId)
            .orElseThrow { NotFoundException("Simulation not found") }

        if (existing.user.username != username) {
            throw ForbiddenException("You do not have access to simulation $simulationId")
        }

        val config = getSimulationConfigBySimulationId(simulationId)

        return submitSimulation(
            username = username,
            params = config.toSimulatorParams(
                zO = existing.zippedOutput,
                pE = existing.peName,
                pP = existing.ppName,
                mp = existing.mpName,
                gFP = null //TODO: handle shared file
            ),
            runSimParser = existing.wMetrics,
            gFile = null,
            label = "Simulation_${existing.id} (Rerun)",
            parentSimulationId = existing.parentSimulationId ?: existing.id!!
        )
    }

    // Helper functions
    // (validations)
    private fun validateBatchRequest(request: CreateSimulationBatchRequest) {
        require(request.label.isNotBlank()) {
            "Label is required for batch simulations"
        }

        require(request.batchSize >= 2) {
            "Batch size must be at least 2"
        }

        require(request.seedMin >= 1) {
            "Minimum seed must be at least 1"
        }

        require(request.seedMax >= request.seedMin) {
            "Maximum seed must be greater than or equal to minimum seed"
        }

        if (!request.randomSeed) {
            val availableSeeds = request.seedMax - request.seedMin + 1
            require(availableSeeds >= request.batchSize) {
                "Seed range is too small for sequential batch generation"
            }
        }
    }


}