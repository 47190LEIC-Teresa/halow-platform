package backend.service

import backend.model.entity.Simulation
import backend.model.entity.SimulationJob
import backend.repository.SimulationJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimulationJobService(
    private val simulationJobRepository: SimulationJobRepository
) {

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
}