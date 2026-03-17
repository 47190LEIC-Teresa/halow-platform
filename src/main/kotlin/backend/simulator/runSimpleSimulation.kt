package backend.simulator
import backend.storage.SimpleSimulationOutputStorage

/**
 * Runs the HaLow simulator, using the provided parameters.
 * (no database interaction, just file storage)
 *
 * This function:
 * 1. Creates a new folder for the simulation run.
 * 2. Launches the Python simulator as a subprocess.
 * 3. Stores all simulator output in a log file.
 * 4. Saves any generated files inside the simulation folder.
 *
 * @param params Parameters passed to the simulator CLI.
 */
fun runSimpleSimulation(params: SimulatorParams) {
    val storage = SimpleSimulationOutputStorage().createSimulationDirectory()

    val runner = HalowSimulatorRunner()
    runner.run(params, storage)

    println("Simulation completed :)")
    println("Results stored in: ${storage.absolutePath}")
}

/*fun main() {
    val params = SimulatorParams(mp= "mp.txt", verbosity = 4)
    println("params: $params")
    runSimpleSimulation(params)
}*/