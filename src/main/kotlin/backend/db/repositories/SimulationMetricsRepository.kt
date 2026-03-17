package backend.db.repositories

import backend.db.entities.Simulation
import backend.db.entities.SimulationMetrics
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationMetricsRepository : JpaRepository<SimulationMetrics, Long>