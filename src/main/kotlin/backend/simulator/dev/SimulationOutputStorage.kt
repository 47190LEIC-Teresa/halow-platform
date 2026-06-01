package backend.simulator.dev

import backend.model.entity.Simulation
import backend.model.entity.SimulationFile
import backend.model.enums.FileType
import org.springframework.stereotype.Component
import java.io.File

@Component
class SimulationOutputStorage (
    private val root: File = File("output/wDB").absoluteFile
) {
    fun createSimulationDirectory(): File {
        val last = root.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("simulations_") }
            ?.mapNotNull { file ->
                file.name.removePrefix("simulations_").toIntOrNull()
            }
            ?.maxOrNull() ?: 0
        val dir = File(root, "simulations/${last + 1}")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    fun buildSimulationFile(
        simulation: Simulation,
        file: File,
        fileType: FileType
    ): SimulationFile {

        return SimulationFile(
            simulation = simulation,
            fileType = fileType,
            fileName = file.name,
            contentType = "application/octet-stream",
            fileSize = file.length(),
            fileData = file.readBytes()
        )
    }
}