package backend.repository

import backend.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
        fun findByUsername(username: String): User?
        fun findByEmail(email: String): User?
        fun existsByUsername(username: String): Boolean
        fun getIdByUsername(username: String): Long?
}