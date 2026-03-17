package backend.simulator

import org.springframework.stereotype.Component
import java.io.File

@Component
class HalowSimulatorRunner : SimulationRunner {

    override fun run(params: SimulatorParams, simulationDir: File): SimulationRunResult {
        val simulatorPath = File("../halowsimulator/halowSimulator.py").absoluteFile.path
        val pythonPath = File("../halowsimulator/venv/bin/python3").absoluteFile.path

        val logFile = File(simulationDir, "log.txt")
        val command = listOf(pythonPath, simulatorPath) + params.toArgs()

        val process = ProcessBuilder(command)
            .directory(simulationDir)
            .redirectErrorStream(true)
            .start()

        logFile.bufferedWriter().use { writer ->
            process.inputStream.bufferedReader().forEachLine { line ->
                writer.write(line)
                writer.newLine()
            }
        }

        val exitCode = process.waitFor()

        return SimulationRunResult(
            exitCode = exitCode,
            logFile = logFile
        )
    }
}