package backend.service

import backend.repository.SimulationFileRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class LogReminderService(
    private val simulationFileRepository: SimulationFileRepository,
    private val emailService: EmailService,
    private val simulationFileService: SimulationFileService
) {

    private val log = LoggerFactory.getLogger(SimulationFileService::class.java)

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun sendLogExpiryReminders() {
        try {
            val now = LocalDateTime.now()
            val reminderThreshold = now.plusHours(1)

            val files = simulationFileRepository.findAboutToExpireLogs(now, reminderThreshold)

            files.forEach { file ->
                emailService.sendLogExpiringReminder(file.simulation, file)
                file.reminderSentAt = LocalDateTime.now()
                simulationFileRepository.save(file)
            }
        } catch (ex: Exception) {
            log.error("Failed to send log expiry reminders", ex)
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun processLogLifecycle() {
        try {
            val now = LocalDateTime.now()
            val reminderThreshold = now.plusHours(1)

            val files = simulationFileRepository.findAboutToExpireLogs(now, reminderThreshold)

            files.forEach { file ->
                emailService.sendLogExpiringReminder(file.simulation, file)
                file.reminderSentAt = LocalDateTime.now()
                simulationFileRepository.save(file)
            }

            simulationFileService.clearExpiredLogFiles()
        } catch (ex: Exception) {
            log.error("Failed to process log lifecycle", ex)
        }
    }
}