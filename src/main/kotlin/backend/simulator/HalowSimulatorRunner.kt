package backend.simulator

import backend.simulator.model.SimulationRunResult
import backend.simulator.model.SimulatorParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files

@Component
class HalowSimulatorRunner (
    @Value("\${simulator.python-path}") private val pythonPath: String,
    @Value("\${simulator.script-path}") private val simulatorPath: String
) : ISimulationRunner{

    override fun run(params: SimulatorParams): SimulationRunResult {
        val command = listOf(pythonPath, simulatorPath) + params.toArgs()

        val simulationDir = createSimulationDirectory()

        val zippedToStdout = params.zippedOutput

        val process = ProcessBuilder(command)
            .directory(simulationDir)
            .redirectErrorStream(false)
            .start()

        val stdoutBytes = process.inputStream.readBytes()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        val fileName = if (zippedToStdout) "log.zip" else "log.txt"

        File(simulationDir, fileName).apply { writeBytes(stdoutBytes) }

        return SimulationRunResult(
            exitCode = exitCode,
            stderr = stderr,
            tempDirectory = simulationDir,
        )
    }

    private fun createSimulationDirectory(): File {
        return Files.createTempDirectory("simulation_run_").toFile()
    }
}