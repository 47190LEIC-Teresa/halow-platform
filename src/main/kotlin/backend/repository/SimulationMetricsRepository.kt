package backend.repository

import backend.model.entity.SimulationMetrics
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationMetricsRepository : JpaRepository<SimulationMetrics, Long> {
    fun findBySimulationId(simulationId: Long): SimulationMetrics?
}