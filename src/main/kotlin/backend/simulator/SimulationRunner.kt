package backend.simulator

import java.io.File

interface SimulationRunner {
    fun run(params: SimulatorParams, simulationDir: File): SimulationRunResult
}