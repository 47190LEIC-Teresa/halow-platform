package backend.simulator

import java.io.File

data class SimulationRunResult (
    val exitCode: Int,
    val logFile: File
)