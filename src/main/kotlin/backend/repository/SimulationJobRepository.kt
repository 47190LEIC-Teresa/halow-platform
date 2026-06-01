package backend.repository

import backend.model.entity.SimulationJob
import backend.model.enums.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SimulationJobRepository : JpaRepository<SimulationJob, Long> {

    @Query(
        value = """
        SELECT sj.*
        FROM simulation_job sj
        JOIN simulation s ON s.id = sj.simulation_id
        WHERE sj.status = CAST(:#{#status.name()} AS varchar)
          AND s.username = :username
        ORDER BY sj.created_at ASC, sj.id ASC
        LIMIT 1
        FOR UPDATE OF sj SKIP LOCKED
    """,
        nativeQuery = true
    )
    fun findOldestPendingJobForUserForUpdate(
        @Param("status") status: JobStatus,
        @Param("username") username: String
    ): SimulationJob?

    @Query(
        value = """
        SELECT DISTINCT s.username
        FROM simulation_job sj
        JOIN simulation s ON s.id = sj.simulation_id
        WHERE sj.status = CAST(:#{#status.name()} AS varchar)
        ORDER BY s.username ASC
    """,
        nativeQuery = true
    )
    fun findUsernamesWithPendingJobs(
        @Param("status") status: JobStatus
    ): List<String>
}