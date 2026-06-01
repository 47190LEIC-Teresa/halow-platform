package backend.model.dto

import backend.model.entity.Simulation
import backend.simulator.model.SimulatorParams

data class SimulationConfigResponse(
    val seed: Int,
    val verbosity: Int,
    val stations: Int,
    val groups: Int,
    val simLength: Long,
    val packetRate: Int,
    val slotLength: Long,
    val label: String,
    val height: Int,
    val width: Int
) {
    fun toSimulatorParams(zO: Boolean, pE: String?, pP: String?, mp: String?, gFP: String?): SimulatorParams = SimulatorParams(
        seed = seed,
        verbosity = verbosity,
        n = stations,
        g = groups,
        simLength = simLength,
        packetRate = packetRate,
        slotLength = slotLength,
        h = height,
        w = width,
        zippedOutput = zO,
        pE = pE,
        pP = pP,
        mp = mp,
        groupsFilePath = gFP
    )
}

fun toConfigResponse(simulation: Simulation): SimulationConfigResponse = SimulationConfigResponse(
    seed = simulation.seed,
    verbosity = simulation.config.verbosity,
    stations = simulation.config.nStations,
    groups = simulation.config.nGroups,
    simLength = simulation.config.simLength,
    packetRate = simulation.config.packetRate,
    slotLength = simulation.config.slotLength,
    label = simulation.label?: "",
    height = simulation.config.height,
    width = simulation.config.width
)