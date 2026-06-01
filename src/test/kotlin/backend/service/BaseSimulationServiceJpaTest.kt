package backend.service

import backend.security.authorization.SimulationSecurity
import backend.model.entity.User
import backend.repository.SimulationConfigRepository
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import backend.repository.UserRepository
import backend.simulator.model.SimulatorParams
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class BaseSimulationServiceJpaTest {

    @Autowired
    protected lateinit var userRepo: UserRepository

    @Autowired
    protected lateinit var simulationRepo: SimulationRepository

    @Autowired
    protected lateinit var configRepo: SimulationConfigRepository

    @Autowired
    protected lateinit var fileRepo: SimulationFileRepository

    protected val simulationSecurity: SimulationSecurity = mockk(relaxed = true)
    protected val jobService: SimulationJobService = mockk(relaxed = true)

    protected val fileService by lazy {
        SimulationFileService(
            simulationFileRepository = fileRepo,
            simulationRepository = simulationRepo,
            simulationSecurity = simulationSecurity
        )
    }

    protected fun buildService() = SimulationService(
        userRepository = userRepo,
        simulationConfigRepository = configRepo,
        simulationRepository = simulationRepo,
        simulationFileService = fileService,
        simulationJobService = jobService
    )

    protected fun params() = SimulatorParams(
        g = 2,
        n = 10,
        w = 100,
        h = 100,
        verbosity = 1,
        simLength = 1000L,
        packetRate = 5,
        slotLength = 50L,
        seed = 123,
        zippedOutput = false,
        pE = null,
        pP = null,
        mp = null,
        groupsFilePath = null
    )

    protected fun createUser(
        username: String = "user_1",
        email: String = "user@test.com"
    ): User = userRepo.saveAndFlush(
        User(
            username = username,
            email = email,
            firstName = "User",
            lastName = "One",
            passwordHash = "hash"
        )
    )
}