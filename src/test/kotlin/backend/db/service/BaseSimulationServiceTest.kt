package backend.db.service

import backend.db.entities.AppUser
import backend.db.repositories.AppUserRepository
import backend.db.repositories.SimulationConfigRepository
import backend.db.repositories.SimulationFileRepository
import backend.db.repositories.SimulationRepository
import backend.simulator.SimulationRunner
import backend.simulator.SimulatorParams
import backend.storage.SimulationOutputStorage
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class BaseSimulationServiceTest {

    @Autowired
    lateinit var userRepo: AppUserRepository

    @Autowired
    lateinit var simulationRepo: SimulationRepository

    @Autowired
    lateinit var configRepo: SimulationConfigRepository

    @Autowired
    lateinit var fileRepo: SimulationFileRepository

    protected val runner = mockk<SimulationRunner>()

    protected fun buildService() = SimulationService(
        userRepo,
        configRepo,
        simulationRepo,
        fileRepo,
        runner,
        SimulationOutputStorage(File("build/test-output/${System.nanoTime()}"))
    )

    protected fun params() = SimulatorParams(
        g = 2,
        n = 10,
        w = 100,
        h = 100,
        verbosity = 1,
        simLength = 1000,
        packetRate = 5,
        slotLength = 50,
        seed = 123,
        mp = null,
        pP = null,
        pE = null,
        fileGroups = null,
        zippedOutput = false
    )

    protected fun createUser(): AppUser {
        return userRepo.saveAndFlush(
            AppUser(
                username = "user_1",
                email = "user@test.com",
                firstName = "User",
                lastName = "One",
                passwordHash = "hash"
            )
        )
    }
}