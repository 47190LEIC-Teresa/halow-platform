package backend.simulator
import kotlin.random.Random

/**
 * Represents the configurable parameters for the HaLow simulator.
 *
 * Each field corresponds to a CLI argument of `halowSimulator.py`.
 * If a value is null, the simulator's default value is used.
 */
data class SimulatorParams (
    val n: Int = 1,                         // Number of stations
    val g: Int = 1,                         // Number of groups
    val h: Int = 1000,                      // Scenario length
    val w: Int = 1000,                      // Scenario width
    val seed: Int =                         // Seed (null = random)
        Random.nextInt(0, 100_000_000),
    val verbosity: Int = 0,                 // Verbosity
    val simLength: Long = 2e7.toLong(),     // Simulation length (ms)
    val packetRate: Int = 10000,            // Average packet rate (packets/us)
    val slotLength: Long = 50e3.toLong(),   // Slot length (ms)
    val zippedOutput: Boolean = false,      // Generate zipped output
    val pE: String? = null,                 // File name for the RSSI of each link
    val pP: String? = null,                 // File name for the stations coordinates
    val mp: String? = null,                 // File to outputs the propagation path loss
    val fileGroups: String? = null          // File name for the grouping file
) {
    fun toArgs(): List<String> = buildList {
        fun arg(flag: String, value: Any) {
            add(flag)
            add(value.toString())
        }

        fun argIfNotNull(flag: String, value: Any?) {
            value?.let { arg(flag, it) }
        }

        arg("-n", n)
        arg("-g", g)
        arg("-H", h)
        arg("-W", w)
        arg("-s", seed)
        arg("-v", verbosity)
        arg("-l", simLength)
        arg("-r", packetRate)
        arg("-S", slotLength)

        if (zippedOutput) add("-z")

        argIfNotNull("-pE", pE)
        argIfNotNull("-pP", pP)
        argIfNotNull("-mp", mp)
        argIfNotNull("-G", fileGroups)
    }
}