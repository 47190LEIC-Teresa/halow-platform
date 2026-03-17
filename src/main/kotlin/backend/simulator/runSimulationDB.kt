
package backend.simulator

import backend.db.entities.AppUser
import backend.db.repositories.AppUserRepository
import backend.db.service.SimulationService
import backend.simulator.SimulatorParams
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@Profile("run-sim")
class SimulationDbRunner(
    private val simulationService: SimulationService,
    private val appUserRepository: AppUserRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            val username = "user_1"

            if (appUserRepository.findByUsername(username) == null) {
                appUserRepository.save(
                    AppUser(
                        username = username,
                        email = "user1@test.com",
                        firstName = "User",
                        lastName = "One",
                        passwordHash = "hash"
                    )
                )
            }

            val params = SimulatorParams(
                mp = "mp.txt",
                verbosity = 4
                // fill the rest
            )

            simulationService.runSimulation(username, params)
            println("Simulation finished through service")
        } finally {
            exitProcess(0)
        }
    }
}