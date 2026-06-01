package backend.exception

open class ConflictException(
    errorCode: ErrorCode,
    vararg args: Any
) : AppException(errorCode, *args)

class UserAlreadyExistsException(username: String) :
    ConflictException(ErrorCode.USER_ALREADY_EXISTS, username)

class EmailAlreadyExistsException(email: String) :
    ConflictException(ErrorCode.USER_EMAIL_ALREADY_EXISTS, email)