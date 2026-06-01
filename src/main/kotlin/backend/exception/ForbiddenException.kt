package backend.exception

open class ForbiddenException(
    errorCode: ErrorCode,
    vararg args: Any
) : AppException(errorCode, *args)

class SimulationAccessDeniedException(id: Long) :
    ForbiddenException(ErrorCode.SIMULATION_ACCESS_DENIED, id)