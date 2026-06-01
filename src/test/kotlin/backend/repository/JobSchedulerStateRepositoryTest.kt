package backend.repository

import backend.model.entity.JobSchedulerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobSchedulerStateRepositoryTest {

    @Autowired
    lateinit var jobSchedulerStateRepository: JobSchedulerStateRepository

    @Test
    fun `findByIdForUpdate should return state when exists`() {
        val saved = jobSchedulerStateRepository.saveAndFlush(
            JobSchedulerState(
                id = 1L,
                lastServedUsername = "alice"
            )
        )

        val found = jobSchedulerStateRepository.findByIdForUpdate(saved.id)

        assertNotNull(found)
        assertEquals("alice", found!!.lastServedUsername)
    }

    @Test
    fun `findByIdForUpdate should return null when state does not exist`() {
        val found = jobSchedulerStateRepository.findByIdForUpdate(999L)

        assertNull(found)
    }
}