package backend.model.dto

import backend.model.enums.LogStatus
import backend.model.enums.SimulationStatus

data class SimulationResponse(
    val simulationId: Long,
    val status: SimulationStatus,
    val logStatus: LogStatus,
    val label: String?,
    val owner: String,
    val createdAt: String,
    val startedAt: String?,
    val finishedAt: String?,
    val parentSimulationId: Long? = null
)