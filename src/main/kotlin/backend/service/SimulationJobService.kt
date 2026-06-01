package backend.service

import backend.exception.JobNotFoundException
import backend.exception.JobSchedulerStateMissingException
import backend.exception.SimulationOutputFileNotFoundException
import backend.model.entity.Simulation
import backend.model.entity.SimulationJob
import backend.model.enums.FileType
import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.JobSchedulerStateRepository
import backend.repository.SimulationJobRepository
import backend.repository.SimulationRepository
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulationRunResult
import backend.simulator.model.SimulatorParams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.io.File
import java.time.LocalDateTime

@Service
class SimulationJobService(
    private val simulationJobRepository: SimulationJobRepository,
    private val simulationRepository: SimulationRepository,
    private val schedulerStateRepository: JobSchedulerStateRepository,
    private val simulationRunner: ISimulationRunner,
    private val simulationFileService: SimulationFileService,
    private val simulationMetricsService: SimulationMetricsService,
    private val emailService: EmailService,
    private val transactionTemplate: TransactionTemplate
) {

    private val log = LoggerFactory.getLogger(SimulationJobService::class.java)

    @Transactional
    fun createJob(
        simulation: Simulation,
        gFileId: Long? = null
    ): SimulationJob {
        val job = SimulationJob(
            simulation = simulation,
        )

        return simulationJobRepository.save(job)
    }

    @Transactional
    fun claimNextPendingJob(): SimulationJob? {
        val state = schedulerStateRepository.findByIdForUpdate(1L)
            ?: throw JobSchedulerStateMissingException()

        val usernames = simulationJobRepository.findUsernamesWithPendingJobs(JobStatus.PENDING)
        if (usernames.isEmpty()) return null

        val orderedUsernames = orderUsernamesStartingAfter(usernames, state.lastServedUsername)

        for (username in orderedUsernames) {
            val job = simulationJobRepository.findOldestPendingJobForUserForUpdate(
                JobStatus.PENDING,
                username
            )

            if (job != null) {
                val now = LocalDateTime.now()
                job.status = JobStatus.RUNNING
                job.startedAt = now

                job.simulation.status = SimulationStatus.RUNNING
                job.simulation.startedAt = now

                state.lastServedUsername = username

                schedulerStateRepository.save(state)
                return simulationJobRepository.save(job)
            }
        }

        return null
    }

    private fun orderUsernamesStartingAfter(
        usernames: List<String>,
        lastServedUsername: String?
    ): List<String> {
        if (usernames.isEmpty()) return emptyList()
        if (lastServedUsername == null) return usernames

        val splitIndex = usernames.indexOfFirst { it > lastServedUsername }
        return if (splitIndex == -1) {
            usernames
        } else {
            usernames.drop(splitIndex) + usernames.take(splitIndex)
        }
    }

    fun processJob(jobId: Long) {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { JobNotFoundException(jobId) }

        if (job.status != JobStatus.RUNNING) return

        val simulation = job.simulation
        val params = buildParamsFromSimulation(simulation)

        try {
            val result = simulationRunner.run(params)

            if (result.exitCode != 0) {
                log.error(
                    "Simulation {} failed with exit code {}. Stderr: {}",
                    simulation.id,
                    result.exitCode,
                    result.stderr
                )
                finalizeJob { markCompleted(jobId, result.exitCode, result.stderr) }
                return
            }

            saveFilesFromResult(simulation, params, result)

            val savedSimulation = finalizeJob { markCompleted(jobId, 0, null) }

            if (savedSimulation.wMetrics) {
                try {
                    simulationMetricsService.runSimulation(savedSimulation.id!!)
                } catch (e: Exception) {
                    log.error("Metrics generation failed for simulation {}", savedSimulation.id, e)
                }
            }
        } catch (e: Exception) {
            log.error("Simulation job {} failed", jobId, e)
            finalizeJob { failJob(jobId, e) }
        }
    }

    private fun markCompleted(jobId: Long, exitCode: Int, errMsg: String?): Simulation {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { JobNotFoundException(jobId) }

        val simulation = job.simulation
        val now = LocalDateTime.now()

        simulation.finishedAt = now
        job.finishedAt = now

        if (exitCode == 0) {
            simulation.status = SimulationStatus.COMPLETED
            simulation.logStatus = LogStatus.READY
            job.status = JobStatus.COMPLETED
        } else {
            val conciseError = extractUserFriendlyError(errMsg)

            simulation.status = SimulationStatus.FAILED
            simulation.logStatus = LogStatus.NOT_READY
            job.status = JobStatus.FAILED
            simulation.errorMsg = "Simulation failed (exit code $exitCode): $conciseError"
            job.errorMsg = "Simulation job failed with exit code $exitCode"
        }

        val savedSimulation = simulationRepository.save(simulation)
        simulationJobRepository.save(job)

        return savedSimulation
    }

    private fun saveFilesFromResult(
        simulation: Simulation,
        params: SimulatorParams,
        result: SimulationRunResult
    ) {
        fun saveFileIfExists(
            fileType: FileType,
            fileName: String? = null,
            finder: (() -> File?)? = null
        ) {
            val file = when {
                finder != null -> finder()
                !fileName.isNullOrBlank() -> File(result.tempDirectory, fileName)
                else -> null
            } ?: return

            if (!file.exists()) {
                throw SimulationOutputFileNotFoundException(fileType, file.name)
            }

            simulationFileService.saveFile(simulation, fileType, file)
        }

        saveFileIfExists(
            FileType.LOG,
            if (params.zippedOutput) "log.zip" else "log.txt"
        )
        saveFileIfExists(FileType.MP, params.mp)
        saveFileIfExists(FileType.PP, params.pP)
        saveFileIfExists(FileType.PE, params.pE)
    }

    private fun failJob(jobId: Long, e: Exception): Simulation {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { JobNotFoundException(jobId) }

        val simulation = job.simulation
        val now = LocalDateTime.now()
        val errorMessage = e.message ?: e::class.simpleName ?: "Unknown error"

        simulation.status = SimulationStatus.FAILED
        simulation.finishedAt = now
        simulation.errorMsg = errorMessage

        job.status = JobStatus.FAILED
        job.finishedAt = now
        job.errorMsg = errorMessage

        val savedSimulation = simulationRepository.save(simulation)
        simulationJobRepository.save(job)

        return savedSimulation
    }

    private fun buildParamsFromSimulation(simulation: Simulation): SimulatorParams {
        val config = simulation.config

        val fileSimId = simulation.parentSimulationId ?: simulation.id!!
        val gFilePath = if (simulation.wGroupFile)
            simulationFileService.createTempGroupsFile(fileSimId) else null

        return SimulatorParams(
            n = config.nStations,
            g = config.nGroups,
            h = config.height,
            w = config.width,
            seed = simulation.seed,
            verbosity = config.verbosity,
            simLength = config.simLength,
            packetRate = config.packetRate,
            slotLength = config.slotLength,
            zippedOutput = simulation.zippedOutput,
            pE = simulation.peName,
            pP = simulation.ppName,
            mp = simulation.mpName,
            groupsFilePath = gFilePath
        )
    }

    private fun finalizeJob(action: () -> Simulation): Simulation {
        val savedSimulation = transactionTemplate.execute { _ ->
            action()
        } ?: throw IllegalStateException("Could not finalize simulation job")

        emailService.sendSimulationFinishedEmail(savedSimulation)
        return savedSimulation
    }

    private fun extractUserFriendlyError(stderr: String?): String {
        if (stderr.isNullOrBlank()) return "No error output provided"

        val lines = stderr
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        return lines.lastOrNull() ?: "No error output provided"
    }
}