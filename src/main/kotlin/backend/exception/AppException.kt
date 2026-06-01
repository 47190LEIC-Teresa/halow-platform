package backend.exception

import backend.model.enums.FileType


open class AppException(
    private val errorCode: ErrorCode,
    vararg args: Any,
    cause: Throwable? = null
) : RuntimeException(
    errorCode.messageTemplate.format(*args),
    cause
) {
    val status = errorCode.status
    val source = errorCode.source
    val code = errorCode.name
}

class PasswordHashFailedException(cause: Throwable? = null) :
    AppException(ErrorCode.PASSWORD_HASH_FAILED, cause = cause)

class SimParserFailedException(exitCode: Int) :
    AppException(ErrorCode.SIM_PARSER_FAILED, exitCode)

class EmptyUploadException :
    AppException(ErrorCode.EMPTY_UPLOAD)

class InvalidMetricsFileTypeException(fileName: String) :
    AppException(ErrorCode.INVALID_METRICS_FILE_TYPE, fileName)

class JobSchedulerStateMissingException :
    AppException(ErrorCode.JOB_SCHEDULER_STATE_MISSING)

class SimulationFileDataMissingException(simulationId: Long, fileType: FileType) :
    AppException(ErrorCode.SIMULATION_FILE_DATA_MISSING, fileType.name, simulationId)

class SimulationFileNoLongerAvailableException(simulationId: Long, fileType: FileType) :
    AppException(ErrorCode.SIMULATION_FILE_NO_LONGER_AVAILABLE, fileType.name, simulationId)

class FileUnavailableException :
    AppException(ErrorCode.FILE_UNAVAILABLE)

class LogFileExpiredException :
    AppException(ErrorCode.LOG_FILE_EXPIRED)

class NoFilesAvailableException(simulationId: Long) :
    AppException(ErrorCode.NO_FILES_AVAILABLE, simulationId)