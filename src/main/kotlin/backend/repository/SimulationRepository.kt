package backend.repository

import backend.model.entity.Simulation
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationRepository : JpaRepository<Simulation, Long>