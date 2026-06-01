package backend.backend.simulator.dev

import backend.backend.model.dto.parseSimulationMetrics
import backend.simulator.SimParserRunner
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@Component
@Profile("run-parser")
// ./gradlew bootRun --args='--spring.profiles.active=run-parser'
class SimParserTestRunner(
    private val simParserRunner: SimParserRunner
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            val logFile = File("../halowsimulator/log").canonicalFile

            if (!logFile.exists()) {
                throw IllegalArgumentException("Log file not found: ${logFile.absolutePath}")
            }

            val result = simParserRunner.run(logFile)

            println("=== SimParser raw output ===")
            println(result.rawOutput)

            val metrics = parseSimulationMetrics(result.rawOutput)

            println("=== Parsed metrics DTO ===")
            println(metrics)

            exitProcess(0)
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}