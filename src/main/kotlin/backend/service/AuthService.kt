package backend.service

import backend.model.dto.LoginRequest
import backend.model.dto.LoginResponse
import backend.model.dto.RegisterUserRequest
import backend.security.jwt.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val jwtUtil: JwtUtil
) {

    @Transactional
    fun register(request: RegisterUserRequest): LoginResponse {
        userService.registerUser(request)

        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username,
                request.password
            )
        )

        val token = jwtUtil.generateToken(auth.name)
        return LoginResponse(token)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.identifier,
                request.password
            )
        )

        val token = jwtUtil.generateToken(auth.name)
        return LoginResponse(token)
    }
}