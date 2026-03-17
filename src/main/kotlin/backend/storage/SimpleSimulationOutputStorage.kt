package backend.storage

import java.io.File

class SimpleSimulationOutputStorage {

    private val root = File("output/simple").absoluteFile

    fun createSimulationDirectory(): File {
        if (!root.exists()) {
            root.mkdirs()
        }

        val nextNumber = (root.listFiles()
            ?.mapNotNull { file ->
                if (file.isDirectory && file.name.startsWith("simulation_")) {
                    file.name.removePrefix("simulation_").toIntOrNull()
                } else {
                    null
                }
            }
            ?.maxOrNull() ?: 0) + 1

        val dir = File(root, "simulation_$nextNumber")
        dir.mkdirs()

        return dir
    }
}