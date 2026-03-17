package backend.storage

import backend.db.entities.Simulation
import backend.db.entities.SimulationFile
import backend.db.enums.FileType
import org.springframework.stereotype.Component
import java.io.File

@Component
class SimulationOutputStorage (
    private val root: File = File("output/wDB").absoluteFile
) {
    fun createSimulationDirectory(simulationId: Long): File {
        val dir = File(root, "simulations/$simulationId")

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

        val relativePath = "simulations/${simulation.id}/${file.name}"

        return SimulationFile(
            simulation = simulation,
            fileType = fileType,
            fileName = file.name,
            fileUrl = relativePath,
            fileSize = file.length()
        )
    }
}