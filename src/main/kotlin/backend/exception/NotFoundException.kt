package backend.exception

import backend.model.enums.FileType

open class NotFoundException(
    errorCode: ErrorCode,
    vararg args: Any
) : AppException(errorCode, *args)

class UserNotFoundException(username: String) :
    NotFoundException(ErrorCode.USER_NOT_FOUND, username)

class SimulationNotFoundException(id: Long) :
    NotFoundException(ErrorCode.SIMULATION_NOT_FOUND, id)

class JobNotFoundException(id: Long) :
    NotFoundException(ErrorCode.JOB_NOT_FOUND, id)

class SimulationOutputFileNotFoundException(fileType: FileType, fileName: String) :
    NotFoundException(ErrorCode.SIMULATION_OUTPUT_FILE_NOT_FOUND, fileType.name, fileName)

class SimulationFileNotFoundException(id: Long) :
    NotFoundException(ErrorCode.SIMULATION_FILE_NOT_FOUND, id)

class SimulationFileMissingException(fileType: FileType, fileName: String) :
    NotFoundException(ErrorCode.SIMULATION_FILE_MISSING, fileType.name, fileName)

class SimulationFileMissingForSimulationException(simulationId: Long, fileType: FileType) :
    NotFoundException(ErrorCode.SIMULATION_FILE_MISSING_FOR_SIMULATION, fileType.name, simulationId)

class SimulationMetricsNotFoundException(simulationId: Long) :
    NotFoundException(ErrorCode.SIMULATION_METRICS_NOT_FOUND, simulationId)