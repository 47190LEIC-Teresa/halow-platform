package backend.model.dto

import backend.model.enums.LogStatus
import backend.model.enums.MetricsStatus
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
    val errorMsg: String? = null,
    val parentSimulationId: Long? = null,
    val metricsStatus: MetricsStatus,
    val metricsErrorMsg: String? = null
)