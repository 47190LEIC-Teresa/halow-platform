package backend.simulator

/**
 * Represents the configurable parameters for the HaLow simulator.
 *
 * Each field corresponds to a CLI argument of `halowSimulator.py`.
 * If a value is null, the simulator's default value is used.
 */
data class SimulatorParams (
    val n: Int? = null,                 // Number of stations
    val g: Int? = null,                 // Number of groups
    val h: Int? = null,                 // Scenario length
    val w: Int? = null,                 // Scenario width
    val seed: Long? = null,             // Seed (null = random)
    val verbosity: Int? = null,         // Verbosity
    val simLength: Long? = null,        // Simulation length (ms)
    val packetRate: Int? = null,        // Average packet rate (packets/us)
    val slotLength: Int? = null,        // Slot length (ms)
    val zippedOutput: Boolean? = null,  // Generate zipped output
    val pE: String? = null,             // File name for the RSSI of each link
    val pP: String? = null,             // File name for the stations coordinates
    val mp: String? = null,             // File to outputs the propagation path loss
    val fileGroups: String? = null      // File name for the grouping file
) {
    fun toArgs(): List<String> = buildList {
        n?.let { add("-n"); add(it.toString()) }
        g?.let { add("-g"); add(it.toString()) }
        h?.let { add("-H"); add(it.toString()) }
        w?.let { add("-W"); add(it.toString()) }
        seed?.let { add("-s"); add(it.toString()) }
        verbosity?.let { add("-v"); add(it.toString()) }
        simLength?.let { add("-l"); add(it.toString()) }
        packetRate?.let { add("-r"); add(it.toString()) }
        slotLength?.let { add("-S"); add(it.toString()) }
        zippedOutput?.let { add("-z"); add(it.toString()) }
        pE?.let { add("-pE"); add(it) }
        pP?.let { add("-pP"); add(it) }
        mp?.let { add("-mp"); add(it) }
        fileGroups?.let { add("-G"); add(it) }
    }
}