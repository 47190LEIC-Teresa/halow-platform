package backend.repository

import backend.model.entity.JobSchedulerState
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JobSchedulerStateRepository : JpaRepository<JobSchedulerState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from JobSchedulerState s where s.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): JobSchedulerState?
}