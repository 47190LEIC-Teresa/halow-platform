package backend.db.repositories

import backend.db.entities.SimulationConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SimulationConfigRepository : JpaRepository<SimulationConfig, Long> {
    @Query("""
        SELECT sc
        FROM SimulationConfig sc
        WHERE sc.nGroups = :nGroups
          AND sc.nStations = :nStations
          AND sc.width = :width
          AND sc.height = :height
          AND sc.verbosity = :verbosity
          AND sc.simLength = :simLength
          AND sc.packetRate = :packetRate
          AND sc.slotLength = :slotLength
    """)
    fun findMatchingConfig(
        @Param("nGroups") nGroups: Int,
        @Param("nStations") nStations: Int,
        @Param("width") width: Int,
        @Param("height") height: Int,
        @Param("verbosity") verbosity: Int,
        @Param("simLength") simLength: Long,
        @Param("packetRate") packetRate: Int,
        @Param("slotLength") slotLength: Long
    ): SimulationConfig?
}
