package backend.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(AppException::class)
    fun handleAppException(
        ex: AppException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        if (ex.status.is5xxServerError) {
            log.error("[{}][{}] {}", ex.source, ex.code, request.requestURI, ex)
        } else {
            log.warn("[{}][{}] {} - {}", ex.source, ex.code, request.requestURI, ex.message)
        }

        return ResponseEntity.status(ex.status).body(
            ApiError(
                status = ex.status.value(),
                error = ex.status.name,
                code = ex.code,
                message = ex.message ?: "Unexpected application error",
                source = ex.source.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Validation failed" }

        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.name,
                code = "VALIDATION_FAILED",
                message = message,
                source = ErrorSource.API.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(
        ex: BadCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiError(
                status = HttpStatus.UNAUTHORIZED.value(),
                error = HttpStatus.UNAUTHORIZED.name,
                code = "INVALID_CREDENTIALS",
                message = "Invalid username or password",
                source = ErrorSource.SECURITY.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(
        ex: AuthorizationDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiError(
                status = HttpStatus.FORBIDDEN.value(),
                error = HttpStatus.FORBIDDEN.name,
                code = "ACCESS_DENIED",
                message = "You do not have permission to perform this action",
                source = ErrorSource.SECURITY.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ApiError(
                status = HttpStatus.PAYLOAD_TOO_LARGE.value(),
                error = HttpStatus.PAYLOAD_TOO_LARGE.name,
                code = "FILE_TOO_LARGE",
                message = "Uploaded file is too large",
                source = ErrorSource.API.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccess(
        ex: DataAccessException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.error("[DATABASE][DATABASE_ERROR] {}", request.requestURI, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = HttpStatus.INTERNAL_SERVER_ERROR.name,
                code = "DATABASE_ERROR",
                message = "Database operation failed",
                source = ErrorSource.DATABASE.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return ResponseEntity.badRequest().body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.name,
                code = "BAD_REQUEST",
                message = ex.message ?: "Invalid request",
                source = ErrorSource.API.name,
                path = request.requestURI
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        log.error("[UNKNOWN][INTERNAL_ERROR] {}", request.requestURI, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiError(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = HttpStatus.INTERNAL_SERVER_ERROR.name,
                code = "INTERNAL_ERROR",
                message = "Server unavailable, please try again later",
                source = ErrorSource.UNKNOWN.name,
                path = request.requestURI
            )
        )
    }
}