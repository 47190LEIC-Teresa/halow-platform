package backend.model.dto

data class RegisterUserRequest(
    val username: String,
    val email: String? = null,
    val firstName: String,
    val lastName: String,
    val password: String
)