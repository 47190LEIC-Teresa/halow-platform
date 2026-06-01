package backend.service

import backend.model.entity.User
import backend.repository.SimulationConfigRepository
import backend.repository.SimulationFileRepository
import backend.repository.SimulationRepository
import backend.repository.UserRepository
import backend.simulator.ISimulationRunner
import backend.simulator.model.SimulatorParams
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class BaseSimulationServiceTest {

    @Autowired
    protected lateinit var userRepo: UserRepository

    @Autowired
    protected lateinit var simulationRepo: SimulationRepository

    @Autowired
    protected lateinit var configRepo: SimulationConfigRepository

    @Autowired
    protected lateinit var fileRepo: SimulationFileRepository

    // Real file service (depends only on repo)
    protected val fileService by lazy { SimulationFileService(fileRepo) }

    // External collaborators mocked
    protected val runner: ISimulationRunner = mockk(relaxed = true)
    protected val jobService: SimulationJobService = mockk(relaxed = true)

    protected fun buildService() = SimulationService(
        userRepository = userRepo,
        simulationConfigRepository = configRepo,
        simulationRepository = simulationRepo,
        simulationFileService = fileService,
        simulationRunner = runner,
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
    ): User =
        userRepo.saveAndFlush(
            User(
                username = username,
                email = email,
                firstName = "User",
                lastName = "One",
                passwordHash = "hash"
            )
        )
}