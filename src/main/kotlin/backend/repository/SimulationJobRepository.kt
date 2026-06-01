package backend.repository

import backend.model.entity.SimulationJob
import backend.model.enums.JobStatus

import org.springframework.data.jpa.repository.JpaRepository

interface SimulationJobRepository : JpaRepository<SimulationJob, Long> {
    fun findFirstByStatusOrderByCreatedAtAsc(status: JobStatus): SimulationJob?
}