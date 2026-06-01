package backend.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val source: ErrorSource,
    val messageTemplate: String
) {
    USER_ALREADY_EXISTS(
        status = HttpStatus.CONFLICT,
        source = ErrorSource.SERVICE,
        messageTemplate = "User already exists: %s"
    ),

    USER_EMAIL_ALREADY_EXISTS(
    status = HttpStatus.CONFLICT,
    source = ErrorSource.SERVICE,
    messageTemplate = "Email already exists: %s"
    ),

    USER_NOT_FOUND(
        status = HttpStatus.NOT_FOUND,
        source = ErrorSource.SERVICE,
        messageTemplate = "User not found: %s"
    ),
    SIMULATION_NOT_FOUND(
        status = HttpStatus.NOT_FOUND,
        source = ErrorSource.SERVICE,
        messageTemplate = "Simulation not found: %s"
    ),
    JOB_NOT_FOUND(
        status = HttpStatus.NOT_FOUND,
        source = ErrorSource.SERVICE,
        messageTemplate = "Job not found: %s"
    ),
    FILE_NOT_FOUND(
        status = HttpStatus.NOT_FOUND,
        source = ErrorSource.SERVICE,
        messageTemplate = "File not found: %s"
    ),
    ACCESS_DENIED(
        status = HttpStatus.FORBIDDEN,
        source = ErrorSource.SECURITY,
        messageTemplate = "You do not have permission to perform this action"
    ),
    PASSWORD_HASH_FAILED(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        source = ErrorSource.SERVICE,
        messageTemplate = "Failed to hash password"
    ),
    INTERNAL_ERROR(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        source = ErrorSource.UNKNOWN,
        messageTemplate = "Server unavailable, please try again later"
    ),
    BAD_REQUEST(
        status = HttpStatus.BAD_REQUEST,
        source = ErrorSource.API,
        messageTemplate = "Invalid request"
    ),
    VALIDATION_FAILED(
        status = HttpStatus.BAD_REQUEST,
        source = ErrorSource.API,
        messageTemplate = "Validation failed"
    ),
    INVALID_CREDENTIALS(
        status = HttpStatus.UNAUTHORIZED,
        source = ErrorSource.SECURITY,
        messageTemplate = "Invalid username or password"
    ),
    SIMULATION_ACCESS_DENIED(
        status = HttpStatus.FORBIDDEN,
        source = ErrorSource.SECURITY,
        messageTemplate = "You do not have access to simulation %s"
    ),
    EMPTY_UPLOAD(
        HttpStatus.BAD_REQUEST,
        ErrorSource.API,
        "Uploaded file is empty"
    ),

    INVALID_METRICS_FILE_TYPE(
        HttpStatus.BAD_REQUEST,
        ErrorSource.API,
        "Only .zip log files are supported: %s"
    ),

    SIM_PARSER_FAILED(
        HttpStatus.UNPROCESSABLE_ENTITY,
        ErrorSource.SERVICE,
        "SimParser failed with exit code %s"
    ),

    SIMULATION_OUTPUT_FILE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        ErrorSource.SERVICE,
        "Simulation output file not found [%s]: %s"
    ),

    JOB_SCHEDULER_STATE_MISSING(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorSource.SERVICE,
        "Job scheduler state row is missing"
    ),

    SIMULATION_FILE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        ErrorSource.SERVICE,
        "Simulation file not found: %s"
    ),

    SIMULATION_FILE_MISSING(
        HttpStatus.NOT_FOUND,
        ErrorSource.SERVICE,
        "%s file not found: %s"
    ),

    SIMULATION_FILE_MISSING_FOR_SIMULATION(
        HttpStatus.NOT_FOUND,
        ErrorSource.SERVICE,
        "%s file not found for simulation %s"
    ),

    SIMULATION_FILE_DATA_MISSING(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorSource.SERVICE,
        "%s file data is missing for simulation %s"
    ),

    SIMULATION_FILE_NO_LONGER_AVAILABLE(
        HttpStatus.GONE,
        ErrorSource.SERVICE,
        "%s file is no longer available for simulation %s"
    ),

    SIMULATION_METRICS_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        ErrorSource.SERVICE,
        "Metrics not found for simulation %s"
    ),

    FILE_UNAVAILABLE(
        HttpStatus.GONE,
        ErrorSource.SERVICE,
        "File is no longer available"
    ),

    LOG_FILE_EXPIRED(
        HttpStatus.GONE,
        ErrorSource.SERVICE,
        "Log file is no longer available for download"
    ),

    NO_FILES_AVAILABLE(
        HttpStatus.GONE,
        ErrorSource.SERVICE,
        "No files available for simulation %s"
    )

}