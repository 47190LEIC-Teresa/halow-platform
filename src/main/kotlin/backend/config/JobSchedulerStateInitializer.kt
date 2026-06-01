package backend.config

import backend.model.entity.JobSchedulerState
import backend.repository.JobSchedulerStateRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class JobSchedulerStateInitializer(
    private val schedulerStateRepository: JobSchedulerStateRepository
) {
    @PostConstruct
    fun init() {
        if (!schedulerStateRepository.existsById(1L)) {
            schedulerStateRepository.save(
                JobSchedulerState(
                    id = 1L,
                    lastServedUsername = null
                )
            )
        }
    }
}