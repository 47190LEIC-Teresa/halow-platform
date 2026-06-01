package backend.simulator

import backend.backend.simulator.ISimulationParserRunner
import backend.simulator.model.SimulationParseRunResult
import org.springframework.stereotype.Component
import java.io.File

@Component
class SimParserRunner: ISimulationParserRunner {

    override fun run(file: File): SimulationParseRunResult {
        val parserPath = File("../halowsimulator/simparser.py").absoluteFile.path
        val pythonPath = File("../halowsimulator/venv/bin/python3").absoluteFile.path

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