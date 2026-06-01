package backend.exception

import java.time.Instant

data class ApiError(
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val source: String,
    val path: String?,
    val timestamp: String = Instant.now().toString()
)