package backend.simulator

import backend.db.SimulationRepository
import java.io.File

fun runSimulation(username: String, params: SimulatorParams) {
    val repository = SimulationRepository()

    println("Creating simulation config for user $username with params: $params")

    val configId = repository.getOrCreateSimulationConfig(params)
    val simulationId = repository.createSimulation(username, configId, params)

    val simulatorPath = File("../halowsimulator/halowSimulator.py").absoluteFile.path
    val pythonPath = File("../halowsimulator/venv/bin/python3").absoluteFile.path
    val command = listOf(pythonPath, simulatorPath) + params.toArgs()

    // Files storage location
    val outputDir = File("output/throughDB").absoluteFile
    val (simulationNumber, simulationDir) = createSimulationFolder(outputDir, simulationId.toInt())
    val logFile = File(simulationDir, "log.txt")

    val logBuffer = StringBuilder()

    println("Starting simulation $simulationId for user $username with config $configId")

    try {
        repository.markSimulationStarted(simulationId)

        val process = ProcessBuilder(command)
            .directory(simulationDir)
            .redirectErrorStream(true)
            .start()

        logFile.bufferedWriter().use { writer ->
            process.inputStream.bufferedReader().forEachLine { line ->
                //println(line)
                logBuffer.appendLine(line)
                writer.write(line)
                writer.newLine()
            }
        }

        val exitCode = process.waitFor()

        val status = if (exitCode == 0) "completed" else "failed"
        repository.markSimulationFinished(simulationId, status)

        // later this could be a real downloadable URL
        val logUrl = "/api/simulations/$simulationId/log"
        repository.createSimulationLog(
            simulationId = simulationId,
            logUrl = logUrl,
            fileSize = logBuffer.length.toLong()
        )

        repository.updateLogStatus(simulationId, "ready")

        println("Simulation $simulationNumber finished with code $exitCode")
        println("Results stored in: ${simulationDir.absolutePath}")
    } catch (e: Exception) {
        repository.markSimulationFinished(simulationId, "aborted")
        throw e
    }
}

fun main() {
    val params = SimulatorParams()
    println("params: $params")
    runSimulation("user_1", params)
}