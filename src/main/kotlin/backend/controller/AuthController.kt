package backend.controller

import backend.model.dto.LoginRequest
import backend.model.dto.LoginResponse
import backend.model.dto.RegisterUserRequest
import backend.service.UserService
import backend.security.JwtUtil
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val jwtUtil: JwtUtil
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterUserRequest): ResponseEntity<LoginResponse> {
        userService.registerUser(request)
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username, request.password
            )
        )
        val token = jwtUtil.generateToken(auth.name)
        return ResponseEntity.status(201).body(LoginResponse(token))
    }



    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.identifier, request.password
            )
        )
        val token = jwtUtil.generateToken(auth.name)
        return ResponseEntity.ok(LoginResponse(token))
    }
}
