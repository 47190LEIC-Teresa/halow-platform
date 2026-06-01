package backend.simulator.dev

import backend.simulator.model.SimulatorParams
import backend.model.entity.User
import backend.repository.UserRepository
import backend.service.SimulationService
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
// This runner will only execute when the "run-sim" profile is active
@Profile("run-sim")
// [./gradlew bootRun --args='--spring.profiles.active=run-sim' to run with this profile]
class SimulationDbRunner(
    private val simulationService: SimulationService,
    private val userRepository: UserRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            val username = "user_1"

            if (userRepository.findByUsername(username) == null) {
                userRepository.save(
                    User(
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
                verbosity = 4,
                zippedOutput = true
                // Change the rest of the parameters as needed for testing
            )

            simulationService.submitSimulation(username, params, false )
            println("Simulation finished through service")
        } finally {
            exitProcess(0)
        }
    }
}