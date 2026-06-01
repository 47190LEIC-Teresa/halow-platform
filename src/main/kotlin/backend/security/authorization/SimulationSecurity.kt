package backend.security.authorization

import backend.repository.SimulationRepository
import org.springframework.stereotype.Component

@Component("simulationSecurity")
class SimulationSecurity(
    private val simulationRepository: SimulationRepository
) {
    fun isOwner(simulationId: Long, username: String): Boolean {
        val simulation = simulationRepository.findById(simulationId).orElse(null) ?: return false
        return simulation.user.username == username
    }
}