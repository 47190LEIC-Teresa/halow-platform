package backend.model.dto

import backend.simulator.model.SimulatorParams

data class CreateSimulationRequest(
    val n: Int,
    val g: Int,
    val h: Int,
    val w: Int,
    val seed: Int,
    val verbosity: Int,
    val simLength: Long,
    val packetRate: Int,
    val slotLength: Long,
    val zippedOutput: Boolean,
    val pE: String?,
    val pP: String?,
    val mp: String?,
    val runSimParser: Boolean = true,
    val label: String? = null
) {
    fun toSimulatorParams(): SimulatorParams =
        SimulatorParams(
            n = n,
            g = g,
            h = h,
            w = w,
            seed = seed,
            verbosity = verbosity,
            simLength = simLength,
            packetRate = packetRate,
            slotLength = slotLength,
            zippedOutput = zippedOutput,
            pE = pE,
            pP = pP,
            mp = mp,
            groupsFilePath = null
        )
}

