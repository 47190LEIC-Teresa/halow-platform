package backend.db.enums

enum class SimulationStatus {
    CREATED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    ABORTED
}