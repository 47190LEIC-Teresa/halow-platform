package backend.controller

import backend.model.dto.UpdateUserRequest
import backend.model.dto.UserResponse
import backend.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService
) {

    @GetMapping
    fun getUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val user = userService.getUserByUsername(authentication.name)
        return ResponseEntity.ok(user)
    }

    @PatchMapping
    fun updateUser(
        @RequestBody request: UpdateUserRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse> {
        val user = userService.updateUser(authentication.name, request)
        return ResponseEntity.ok(user)
    }

}