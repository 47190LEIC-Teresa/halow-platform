package backend.backend.simulator

import backend.simulator.model.SimulationParseRunResult
import java.io.File

interface ISimulationParserRunner {
    fun run(file: File): SimulationParseRunResult
}
