package backend.db.repositories

import backend.db.entities.AppUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserRepositoryTest {

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    private fun buildUser(
        username: String = "user_1",
        email: String = "user1@test.com",
        firstName: String = "User",
        lastName: String = "One",
        passwordHash: String = "hash",
        lastAccess: LocalDateTime? = null
    ) = AppUser(
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        passwordHash = passwordHash,
        lastAccess = lastAccess
    )

    @Test
    fun `should save user and generate id`() {
        val saved = appUserRepository.saveAndFlush(buildUser())

        assertNotNull(saved.id)
    }

    @Test
    fun `should find user by username`() {
        appUserRepository.saveAndFlush(buildUser(username = "geronimo"))

        val found = appUserRepository.findByUsername("geronimo")

        assertEquals("geronimo", found?.username)
    }

    @Test
    fun `should return null when username does not exist`() {
        val found = appUserRepository.findByUsername("missing_user")

        assertNull(found)
    }

    @Test
    fun `should update user fields`() {
        val saved = appUserRepository.saveAndFlush(buildUser())

        val found = appUserRepository.findById(saved.id!!).orElseThrow()
        found.email = "updated@test.com"
        found.firstName = "Updated"
        found.lastName = "User"
        found.lastAccess = LocalDateTime.of(2026, 1, 1, 10, 0)

        appUserRepository.saveAndFlush(found)

        val updated = appUserRepository.findById(saved.id!!).orElseThrow()
        assertEquals("updated@test.com", updated.email)
        assertEquals("Updated", updated.firstName)
        assertEquals("User", updated.lastName)
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), updated.lastAccess)
    }

    @Test
    fun `should delete user by id`() {
        val saved = appUserRepository.saveAndFlush(buildUser())

        appUserRepository.deleteById(saved.id!!)
        appUserRepository.flush()

        val found = appUserRepository.findById(saved.id!!)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should fail when saving duplicate username`() {
        appUserRepository.saveAndFlush(buildUser(username = "same_user", email = "one@test.com"))

        assertThrows<DataIntegrityViolationException> {
            appUserRepository.saveAndFlush(buildUser(username = "same_user", email = "two@test.com"))
        }
    }

    @Test
    fun `should fail when saving duplicate email`() {
        appUserRepository.saveAndFlush(buildUser(username = "user_a", email = "same@test.com"))

        assertThrows<DataIntegrityViolationException> {
            appUserRepository.saveAndFlush(buildUser(username = "user_b", email = "same@test.com"))
        }
    }

    @Test
    fun `should save user with null last access`() {
        val saved = appUserRepository.saveAndFlush(buildUser(lastAccess = null))

        val found = appUserRepository.findById(saved.id!!).orElseThrow()

        assertNull(found.lastAccess)
    }
}