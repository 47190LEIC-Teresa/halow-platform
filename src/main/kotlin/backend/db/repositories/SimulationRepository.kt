package backend.db.repositories

import backend.db.entities.Simulation
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationRepository : JpaRepository<Simulation, Long>