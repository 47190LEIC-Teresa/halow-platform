package backend.db.repositories

import backend.db.entities.SimulationFile
import org.springframework.data.jpa.repository.JpaRepository

interface SimulationFileRepository : JpaRepository<SimulationFile, Long>