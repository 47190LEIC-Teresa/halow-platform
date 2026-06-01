package backend.simulator

import backend.simulator.model.SimulationParseRunResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

@Component
class SimParserRunner(
    @Value("\${simulator.python-path}") private val pythonPath: String,
    @Value("\${simulator.parser-path}") private val parserPath: String
): ISimulationParserRunner {

    override fun run(file: File): SimulationParseRunResult {
        require(File(parserPath).exists()) { "simParser script not found at $parserPath" }
        require(File(pythonPath).exists()) { "Python not found at $pythonPath" }
        require(file.exists()) { "Log file not found at ${file.absolutePath}" }

        val command = listOf(
            pythonPath,
            parserPath,
            "-f",
            file.absolutePath
        )

        println("Running simParser command: ${command.joinToString(" ")}")

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        println("SimParser output:\n$output")

        if (exitCode != 0) {
            throw RuntimeException("SimParser failed with exit code $exitCode.\n$output")
        }

        return SimulationParseRunResult(
            exitCode = exitCode,
            rawOutput = output
        )
    }
}