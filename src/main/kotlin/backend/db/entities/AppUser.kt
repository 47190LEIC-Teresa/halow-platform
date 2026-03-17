package backend.db.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "app_user")
class AppUser(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "username", length = 50, unique = true, nullable = false)
    var username: String,

    @Column(name = "password_hash",  length = 50, nullable = false)
    var passwordHash: String,

    @Column(name = "email", columnDefinition = "varchar(255) CHECK (char_length(email) >= 3)",  unique = true, nullable = false)
    var email: String,

    @Column(name = "first_name", columnDefinition = "varchar(255) CHECK (char_length(email) >= 3)", nullable = false)
    var firstName: String,

    @Column(name = "last_name", columnDefinition = "varchar(255) CHECK (char_length(email) >= 3)", nullable = false)
    var lastName: String,

    @Column(name = "last_access")
    var lastAccess: java.time.LocalDateTime? = null
)