package backend.service

import backend.exception.PasswordHashFailedException
import backend.exception.UserAlreadyExistsException
import backend.exception.UserNotFoundException
import backend.model.dto.RegisterUserRequest
import backend.model.dto.UpdateUserRequest
import backend.model.entity.User
import backend.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var service: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mock()
        passwordEncoder = mock()
        service = UserService(userRepository, passwordEncoder)
    }

    @Test
    fun `loadUserByUsername should find by username`() {
        val user = buildUser()

        whenever(userRepository.findByUsername("john")).thenReturn(user)

        val result = service.loadUserByUsername("john")

        assertEquals("john", result.username)
        assertEquals("hash", result.password)
    }

    @Test
    fun `loadUserByUsername should fall back to email`() {
        val user = buildUser()

        whenever(userRepository.findByUsername("john@test.com")).thenReturn(null)
        whenever(userRepository.findByEmail("john@test.com")).thenReturn(user)

        val result = service.loadUserByUsername("john@test.com")

        assertEquals("john", result.username)
    }

    @Test
    fun `loadUserByUsername should throw when user not found`() {
        whenever(userRepository.findByUsername("missing")).thenReturn(null)
        whenever(userRepository.findByEmail("missing")).thenReturn(null)

        assertThrows(UsernameNotFoundException::class.java) {
            service.loadUserByUsername("missing")
        }
    }

    @Test
    fun `registerUser should throw when username already exists`() {
        whenever(userRepository.findByUsername("john")).thenReturn(buildUser())

        assertThrows(UserAlreadyExistsException::class.java) {
            service.registerUser(
                RegisterUserRequest(
                    username = "john",
                    email = "john@test.com",
                    password = "secret",
                    firstName = "John",
                    lastName = "Doe"
                )
            )
        }
    }

    @Test
    fun `registerUser should encode password and save`() {
        whenever(userRepository.findByUsername("john")).thenReturn(null)
        whenever(passwordEncoder.encode("secret")).thenReturn("encoded")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        val response = service.registerUser(
            RegisterUserRequest(
                username = "john",
                email = "john@test.com",
                password = "secret",
                firstName = "John",
                lastName = "Doe"
            )
        )

        assertEquals("john", response.username)
        assertEquals("john@test.com", response.email)
        assertEquals("John", response.firstName)
        assertEquals("Doe", response.lastName)
    }

    @Test
    fun `getUserByUsername should throw when user missing`() {
        whenever(userRepository.findByUsername("missing")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            service.getUserByUsername("missing")
        }
    }

    @Test
    fun `updateUser should update provided fields`() {
        val user = buildUser()

        whenever(userRepository.findByUsername("john")).thenReturn(user)
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        val response = service.updateUser(
            "john",
            UpdateUserRequest(
                email = "new@test.com",
                firstName = "New",
                lastName = "Name",
                password = null
            )
        )

        assertEquals("new@test.com", response.email)
        assertEquals("New", response.firstName)
        assertEquals("Name", response.lastName)
    }

    @Test
    fun `updateUser should wrap password encoding failure`() {
        val user = buildUser()

        whenever(userRepository.findByUsername("john")).thenReturn(user)
        whenever(passwordEncoder.encode("new-secret")).thenThrow(RuntimeException("encode failed"))

        assertThrows(PasswordHashFailedException::class.java) {
            service.updateUser(
                "john",
                UpdateUserRequest(
                    email = null,
                    firstName = null,
                    lastName = null,
                    password = "new-secret"
                )
            )
        }
    }

    private fun buildUser(): User {
        return User(
            id = 1L,
            username = "john",
            passwordHash = "hash",
            email = "john@test.com",
            firstName = "John",
            lastName = "Doe",
            lastAccess = null
        )
    }
}