package backend.controller.admin

import backend.model.dto.RegisterUserRequest
import backend.model.dto.UpdateUserRequest
import backend.model.dto.UserResponse
import backend.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val userService: UserService
) {

    @GetMapping
    fun getAllUsers(): List<UserResponse> {
        return userService.getAllUsers()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@RequestBody request: RegisterUserRequest) =
        userService.registerUser(request)


    @PatchMapping("/{username}")
    fun updateUser(
        @RequestBody request: UpdateUserRequest,
        authentication: Authentication,
        @PathVariable username: String
    ): ResponseEntity<UserResponse> {
        val user = userService.updateUser(username, request)
        return ResponseEntity.ok(user)
    }

}