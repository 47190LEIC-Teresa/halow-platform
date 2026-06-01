package backend.model.dto

data class UserResponse(
    val username: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?
)