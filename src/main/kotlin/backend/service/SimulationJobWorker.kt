package backend.service

import backend.backend.service.SimulationMetricsService
import backend.model.entity.Simulation
import backend.model.entity.SimulationJob
import backend.model.enums.FileType
import backend.model.enums.JobStatus
import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus
import backend.repository.SimulationJobRepository
import backend.repository.SimulationRepository
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulationRunResult
import backend.simulator.model.SimulatorParams
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime

@Service
class SimulationJobWorker(
    private val simulationJobRepository: SimulationJobRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationRunner: ISimulationRunner,
    private val simulationFileService: SimulationFileService,
    private val simulationMetricsService: SimulationMetricsService
    ) {

    private val log = LoggerFactory.getLogger(SimulationJobWorker::class.java)

    @Scheduled(fixedDelay = 3000)
    fun pollPendingJobs() {
        val job = simulationJobRepository.findFirstByStatusOrderByCreatedAtAsc(JobStatus.PENDING)
            ?: return

        processJob(job.id!!)
    }

    @Transactional
    fun markJobRunning(jobId: Long): SimulationJob {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job not found: $jobId") }

        if (job.status != JobStatus.PENDING) return job

        job.status = JobStatus.RUNNING
        job.startedAt = LocalDateTime.now()

        val simulation = job.simulation
        simulation.status = SimulationStatus.RUNNING
        simulation.startedAt = LocalDateTime.now()

        simulationRepository.save(simulation)
        return simulationJobRepository.save(job)
    }

    @Transactional
    fun processJob(jobId: Long) {
        val job = markJobRunning(jobId)
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
                // TODO: handle message error better, maybe with a custom exception type
                markCompleted(jobId, result.exitCode)
                return
            }

            saveFilesFromResult(simulation, params, result)

            if (simulation.wMetrics) {
                simulationMetricsService.runSimulation(simulation.id!!)
            }

            markCompleted(jobId, 0)
        } catch (e: Exception) {
            failJob(jobId, e)
        }
    }

    @Transactional
    fun markCompleted(jobId: Long, exitCode: Int) {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job not found: $jobId") }

        val simulation = job.simulation
        val now = LocalDateTime.now()

        simulation.finishedAt = now
        job.finishedAt = now

        if (exitCode == 0) {
            simulation.status = SimulationStatus.COMPLETED
            simulation.logStatus = LogStatus.READY
            job.status = JobStatus.COMPLETED
        } else {
            simulation.status = SimulationStatus.FAILED
            job.status = JobStatus.FAILED
        }

        simulationRepository.save(simulation)
        simulationJobRepository.save(job)
    }

    fun saveFilesFromResult(
        simulation: Simulation,
        params: SimulatorParams,
        result: SimulationRunResult
    ) {
        fun saveFileIfExists (
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
                throw IllegalArgumentException("$fileType file not found: ${file.name}")
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



    @Transactional
    fun failJob(jobId: Long, e: Exception) {
        val job = simulationJobRepository.findById(jobId)
            .orElseThrow { IllegalArgumentException("Job not found: $jobId") }

        val simulation = job.simulation
        val now = LocalDateTime.now()

        simulation.status = SimulationStatus.FAILED
        simulation.finishedAt = now
        simulation.errorMsg = e.message

        job.status = JobStatus.FAILED
        job.finishedAt = now
        job.errorMsg = e.message

        simulationRepository.save(simulation)
        simulationJobRepository.save(job)
    }

    private fun buildParamsFromSimulation(simulation: Simulation): SimulatorParams {
        val config = simulation.config

        val fileSimId = simulation.parentSimulationId ?: simulation.id!!
        val gFilePath =  if (simulation.wGroupFile)
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
}