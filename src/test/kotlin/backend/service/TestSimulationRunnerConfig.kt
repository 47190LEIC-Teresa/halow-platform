package backend.service

import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulationRunResult
import backend.simulator.model.SimulatorParams
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.io.File

@TestConfiguration
class TestSimulationRunnerConfig {

    @Bean
    fun testSimulationRunner(): ISimulationRunner = object : ISimulationRunner {
        override fun run(params: SimulatorParams): SimulationRunResult {
            // Create a temporary directory for this run
            val tempDir = createTempDir(prefix = "sim-run-", suffix = null)

            // Always create a log file, so saveFilesFromResult() succeeds
            val logFileName = if (params.zippedOutput) "log.zip" else "log.txt"
            val logFile = File(tempDir, logFileName)

            if (params.zippedOutput) {
                // Minimal zip: just create empty file; your service only checks existence
                logFile.writeBytes(ByteArray(0))
            } else {
                logFile.writeText("fake log content for testing")
            }

            // Optionally simulate work by sleeping a bit
            // Thread.sleep(50)

            return SimulationRunResult(
                exitCode = 0,
                stderr = null,
                tempDirectory = tempDir
            )
        }
    }
}