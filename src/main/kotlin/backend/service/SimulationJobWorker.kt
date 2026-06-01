package backend.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SimulationJobWorker(
    private val simulationJobExecutor: java.util.concurrent.Executor,
    private val simulationJobService: SimulationJobService,
) {

    private val log = LoggerFactory.getLogger(SimulationJobWorker::class.java)

    @Scheduled(fixedDelay = 3000)
    fun pollPendingJobs() {
        val claimedJob = simulationJobService.claimNextPendingJob() ?: return
        val jobId = claimedJob.id ?: return

        simulationJobExecutor.execute {
            try {
                simulationJobService.processJob(jobId)
            } catch (ex: Exception) {
                log.error("Unhandled exception while processing simulation job {}", jobId, ex)
            }
        }
    }
}