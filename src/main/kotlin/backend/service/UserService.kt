package backend.service

import backend.model.dto.UserResponse
import backend.model.dto.UpdateUserRequest
import backend.model.entity.User
import backend.model.dto.RegisterUserRequest
import backend.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.crypto.password.PasswordEncoder

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
): UserDetailsService {

    private fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName
        )
    }

    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {    // Try to find by username first, then by email
        val user = userRepository.findByUsername(username)
            ?: userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.username)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_USER"))
            .build()
    }


    @Transactional
    fun registerUser(request: RegisterUserRequest): UserResponse {
        if (userRepository.findByUsername(request.username) != null) {
            throw IllegalArgumentException("User already exists: ${request.username}")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName
        )

        val savedUser = userRepository.save(user)

        return toUserResponse(savedUser)
    }

    @Transactional(readOnly = true)
    fun getUserByUsername(username: String): UserResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")

        return toUserResponse(user)
    }

    @Transactional(readOnly = true)
    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { user ->
            toUserResponse(user)
        }
    }

    @Transactional
    fun updateUser(username: String, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")

        request.email?.let { user.email = it }
        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }

        request.password?.let {
            try {
                user.passwordHash = passwordEncoder.encode(it)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to hash password")
            }
        }

        val saved = userRepository.save(user)

        return toUserResponse(saved)
    }

}