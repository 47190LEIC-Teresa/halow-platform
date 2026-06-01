package backend.simulator

import backend.simulator.model.SimulationRunResult
import backend.simulator.model.SimulatorParams

interface ISimulationRunner {
    fun run(params: SimulatorParams): SimulationRunResult
}
