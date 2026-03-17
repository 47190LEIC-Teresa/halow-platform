package backend.db.repositories

import backend.db.entities.AppUser
import backend.db.entities.Simulation
import backend.db.entities.SimulationConfig
import backend.db.enums.LogStatus
import backend.db.enums.SimulationStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SimulationRepositoryTest {

    @Autowired
    lateinit var simulationRepository: SimulationRepository

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    @Autowired
    lateinit var simulationConfigRepository: SimulationConfigRepository

    private fun buildUser(
        username: String = "user_1",
        email: String = "user1@test.com",
        firstName: String = "User",
        lastName: String = "One",
        passwordHash: String = "hash",
        lastAccess: LocalDateTime? = null
    ): AppUser {
        return AppUser(
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            passwordHash = passwordHash,
            lastAccess = lastAccess
        )
    }

    private fun buildSimulationConfig(
        nGroups: Int = 2,
        nStations: Int = 10,
        width: Int = 100,
        height: Int = 200,
        verbosity: Int = 1,
        simLength: Long = 1000L,
        packetRate: Int = 5,
        slotLength: Long = 50L
    ): SimulationConfig {
        return SimulationConfig(
            nGroups = nGroups,
            nStations = nStations,
            width = width,
            height = height,
            verbosity = verbosity,
            simLength = simLength,
            packetRate = packetRate,
            slotLength = slotLength
        )
    }

    private fun buildSimulation(
        user: AppUser,
        config: SimulationConfig,
        status: SimulationStatus = SimulationStatus.QUEUED,
        logStatus: LogStatus = LogStatus.NOT_READY,
        errorMsg: String? = null,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 1, 1, 10, 0),
        startedAt: LocalDateTime? = null,
        finishedAt: LocalDateTime? = null,
        seed: Int = 123,
        wPe: Boolean = false,
        wPp: Boolean = false,
        wMp: Boolean = false,
        wGroupFile: Boolean = false,
        wMetrics: Boolean = false,
        zippedOutput: Boolean = false
    ): Simulation {
        return Simulation(
            user = user,
            config = config,
            status = status,
            logStatus = logStatus,
            errorMsg = errorMsg,
            createdAt = createdAt,
            startedAt = startedAt,
            finishedAt = finishedAt,
            seed = seed,
            wPe = wPe,
            wPp = wPp,
            wMp = wMp,
            wGroupFile = wGroupFile,
            wMetrics = wMetrics,
            zippedOutput = zippedOutput
        )
    }

    @Test
    fun `should save simulation and generate id`() {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        val saved = simulationRepository.saveAndFlush(buildSimulation(user, config))

        assertNotNull(saved.id)
    }

    @Test
    fun `should save and retrieve simulation`() {
        val user = appUserRepository.saveAndFlush(buildUser(username = "teresa"))
        val config = simulationConfigRepository.saveAndFlush(
            buildSimulationConfig(nGroups = 4, nStations = 25)
        )

        val createdAt = LocalDateTime.of(2026, 2, 1, 9, 30)

        val saved = simulationRepository.saveAndFlush(
            buildSimulation(
                user = user,
                config = config,
                status = SimulationStatus.RUNNING,
                logStatus = LogStatus.READY,
                errorMsg = null,
                createdAt = createdAt,
                seed = 999,
                wPe = true,
                wPp = true,
                wMp = false,
                wGroupFile = true,
                wMetrics = true,
                zippedOutput = false
            )
        )

        val found = simulationRepository.findById(saved.id!!).orElseThrow()

        assertEquals("teresa", found.user.username)
        assertEquals(config.id, found.config.id)
        assertEquals(SimulationStatus.RUNNING, found.status)
        assertEquals(LogStatus.READY, found.logStatus)
        assertNull(found.errorMsg)
        assertEquals(createdAt, found.createdAt)
        assertEquals(999, found.seed)
        assertTrue(found.wPe)
        assertTrue(found.wPp)
        assertFalse(found.wMp)
        assertTrue(found.wGroupFile)
        assertTrue(found.wMetrics)
        assertFalse(found.zippedOutput)
    }

    @Test
    fun `should update simulation fields`() {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        val saved = simulationRepository.saveAndFlush(buildSimulation(user, config))

        val found = simulationRepository.findById(saved.id!!).orElseThrow()
        found.status = SimulationStatus.COMPLETED
        found.logStatus = LogStatus.READY
        found.errorMsg = "no error"
        found.startedAt = LocalDateTime.of(2026, 1, 1, 10, 5)
        found.finishedAt = LocalDateTime.of(2026, 1, 1, 10, 30)
        found.wPe = true
        found.wMetrics = true
        found.zippedOutput = true

        simulationRepository.saveAndFlush(found)

        val updated = simulationRepository.findById(saved.id!!).orElseThrow()

        assertEquals(SimulationStatus.COMPLETED, updated.status)
        assertEquals(LogStatus.READY, updated.logStatus)
        assertEquals("no error", updated.errorMsg)
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 5), updated.startedAt)
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 30), updated.finishedAt)
        assertTrue(updated.wPe)
        assertTrue(updated.wMetrics)
        assertTrue(updated.zippedOutput)
    }

    @Test
    fun `should save simulation with nullable fields as null`() {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildSimulationConfig())

        val saved = simulationRepository.saveAndFlush(
            buildSimulation(
                user = user,
                config = config,
                errorMsg = null,
                startedAt = null,
                finishedAt = null
            )
        )

        val found = simulationRepository.findById(saved.id!!).orElseThrow()

        assertNull(found.errorMsg)
        assertNull(found.startedAt)
        assertNull(found.finishedAt)
    }

    @Test
    fun `should delete simulation by id`() {
        val user = appUserRepository.saveAndFlush(buildUser())
        val config = simulationConfigRepository.saveAndFlush(buildSimulationConfig())
        val saved = simulationRepository.saveAndFlush(buildSimulation(user, config))

        simulationRepository.deleteById(saved.id!!)
        simulationRepository.flush()

        val found = simulationRepository.findById(saved.id!!)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should return empty when simulation does not exist`() {
        val found = simulationRepository.findById(999999L)

        assertTrue(found.isEmpty)
    }

    @Test
    fun `should persist user and config relations`() {
        val user = appUserRepository.saveAndFlush(buildUser(username = "owner_user"))
        val config = simulationConfigRepository.saveAndFlush(buildSimulationConfig())
        val saved = simulationRepository.saveAndFlush(buildSimulation(user, config))

        val found = simulationRepository.findById(saved.id!!).orElseThrow()

        assertEquals("owner_user", found.user.username)
        assertEquals(config.id, found.config.id)
    }
}