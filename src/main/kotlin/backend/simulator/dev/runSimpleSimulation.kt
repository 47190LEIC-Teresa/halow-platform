package backend.simulator.dev

import backend.simulator.model.SimulatorParams
import backend.simulator.ISimulationRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

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



@Component
@Profile("run-simple-sim")
class SimpleSimulationRunner(
    private val simulationRunner: ISimulationRunner
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            val params = SimulatorParams(
                mp = "mp.txt",
                verbosity = 4,
                zippedOutput = true
            )

            val result = simulationRunner.run(params)

            val outputDir = File("simulation-output-test").absoluteFile
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
            result.tempDirectory.copyRecursively(outputDir, overwrite = true)

            println("Simulation completed :)")
            println("Copied results to: ${outputDir.absolutePath}")
            result.tempDirectory.listFiles()?.forEach {
                println("Generated file: ${it.name}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            exitProcess(0)
        }
    }
}