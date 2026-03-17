package backend.db.repositories

import backend.db.entities.AppUser
import backend.db.entities.SimulationConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AppUserRepository : JpaRepository<AppUser, Long> {
        fun findByUsername(username: String): AppUser?
        fun existsByUsername(username: String): Boolean
        fun getIdByUsername(username: String): Long?
}