package backend.simulator.model

import java.io.File

data class SimulationRunResult (
    val exitCode: Int,
    val stderr: String?,
    val tempDirectory: File
)