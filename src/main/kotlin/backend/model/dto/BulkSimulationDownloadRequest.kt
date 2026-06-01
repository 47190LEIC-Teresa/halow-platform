package backend.model.dto

data class BulkSimulationDownloadRequest(
    val simulationIds: List<Long>
)