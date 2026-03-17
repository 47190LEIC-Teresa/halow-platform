package backend

import backend.db.entities.AppUser
import backend.db.repositories.AppUserRepository
//import backend.service.SimulationService
import backend.simulator.SimulatorParams
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestRunner(
    private val appUserRepository: AppUserRepository,
    //private val simulationService: SimulationService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val username = "user_1"

        val user = appUserRepository.findByUsername(username)
            ?: appUserRepository.save(
                AppUser(
                    username = username,
                    email = "user1@test.com",
                    firstName = "User",
                    lastName = "One",
                    passwordHash = "test"
                )
            )


        val params = SimulatorParams()

        println("Testing JPA + simulator with user ${user.username}")
        //simulationService.runSimulation(user.username, params)
    }
}