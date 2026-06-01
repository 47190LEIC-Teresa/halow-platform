package backend.exception

enum class ErrorSource {
    API,
    SERVICE,
    DATABASE,
    SECURITY,
    UNKNOWN,
}