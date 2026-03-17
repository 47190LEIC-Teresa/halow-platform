package backend.simulator
import java.io.File
import java.util.Objects.isNull

/**
 * Creates a new folder for storing simulation results.
 *
 * The folder is created inside the given base directory and follows
 * the pattern: `simulation_<number>`.
 *
 * Example:
 * output/
 *   simulation_1/
 *   simulation_2/
 *
 * @param baseOutputDir Root folder where simulation folders are stored.
 * @return Pair containing the simulation number and the created directory.
 */
fun createSimulationFolder(baseOutputDir: File, id: Int? = null): Pair<Int, File> {
    if (!baseOutputDir.exists()) {
        baseOutputDir.mkdirs()
    }

    val existingNumbers = baseOutputDir.listFiles()
        ?.mapNotNull { file ->
            if (file.isDirectory && file.name.startsWith("simulation_")) {
                file.name.removePrefix("simulation_").toIntOrNull()
            } else {
                null
            }
        }
        ?: emptyList()

    val nextNumber = id ?: ((existingNumbers.maxOrNull() ?: 0) + 1)
    val simulationDir = File(baseOutputDir, "simulation_$nextNumber")
    simulationDir.mkdirs()

    return nextNumber to simulationDir
}