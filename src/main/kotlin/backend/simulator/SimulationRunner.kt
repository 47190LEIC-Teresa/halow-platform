package backend.simulator
import java.io.File


/**
 * Runs the HaLow simulator locally using the provided parameters.
 *
 * This function:
 * 1. Creates a new folder for the simulation run.
 * 2. Launches the Python simulator as a subprocess.
 * 3. Stores all simulator output in a log file.
 * 4. Saves any generated files inside the simulation folder.
 *
 * @param params Parameters passed to the simulator CLI.
 */
fun runSimulator( params: SimulatorParams) {
    val simulatorPath = File("../halowsimulator/halowSimulator.py").absoluteFile.path
    val pythonPath = File("../halowsimulator/venv/bin/python3").absoluteFile.path

    // Files storage location
    val outputDir = File("output").absoluteFile
    val (simulationNumber, simulationDir) = createSimulationFolder(outputDir)
    val logFile = File(simulationDir, "log.txt")

    val command = listOf(pythonPath, simulatorPath)  + params.toArgs()

    // Start the process
    val process = ProcessBuilder(command)
        .directory(simulationDir)
        .redirectErrorStream(true) // merge stdout and stderr
        .start()

    logFile.bufferedWriter().use { writer ->
        process.inputStream.bufferedReader().forEachLine { line ->
            //println(line)
            writer.write(line)
            writer.newLine()
        }
    }

    // Wait for the process to finish
    val exitCode = process.waitFor()
    println("Simulation $simulationNumber finished with code $exitCode")
    println("Results stored in: ${simulationDir.absolutePath}")
}

fun main() {
    runSimulator(SimulatorParams())
}