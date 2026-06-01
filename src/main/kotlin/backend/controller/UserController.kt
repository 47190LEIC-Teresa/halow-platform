package backend.controller

import backend.model.dto.UpdateUserRequest
import backend.model.dto.UserResponse
import backend.service.UserService
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    fun getUser(authentication: Authentication): UserResponse {
        return userService.getUserByUsername(authentication.name)
    }

    @PatchMapping
    fun updateUser(
        @Valid @RequestBody request: UpdateUserRequest,
        authentication: Authentication
    ): UserResponse {
        return userService.updateUser(authentication.name, request)
    }
}