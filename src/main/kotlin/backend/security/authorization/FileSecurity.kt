package backend.security.authorization

import backend.repository.SimulationFileRepository
import org.springframework.stereotype.Component

@Component("fileSecurity")
class FileSecurity(
    private val simulationFileRepository: SimulationFileRepository
) {
    fun canDownload(fileId: Long, username: String): Boolean {
        val file = simulationFileRepository.findById(fileId).orElse(null) ?: return false
        return file.simulation.user.username == username
    }
}