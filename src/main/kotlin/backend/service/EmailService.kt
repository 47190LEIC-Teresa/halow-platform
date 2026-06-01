package backend.service

import backend.model.entity.Simulation
import backend.model.entity.SimulationFile
import backend.model.enums.LogStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.email.enabled:true}") private val emailEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendTestEmail() {
        if (!emailEnabled) {
            log.info("Email sending disabled; skipping test email")
            return
        }

        val message = SimpleMailMessage().apply {
            from = "noreply@halowsimulations.com"
            setTo("test@example.com")
            subject = "Mailtrap test"
            text = "This is a development test email from HaLow Platform."
        }

        mailSender.send(message)
    }

    fun sendSimulationFinishedEmail(simulation: Simulation) {
        if (!emailEnabled) {
            log.info("Email sending disabled; skipping simulation finished email for simulation {}", simulation.id)
            return
        }

        val recipient = getRecipientOrLog(simulation, "simulation finished email") ?: return

        val simulationName = simulation.label?.takeIf { it.isNotBlank() }
            ?: "Simulation #${simulation.id}"

        val lines = mutableListOf(
            "Hello ${simulation.user.username},",
            "",
            "Your simulation \"$simulationName\" has finished.",
            "",
            "Simulation ID: ${simulation.id}",
            "Status: ${simulation.status}"
        )

        if (!simulation.errorMsg.isNullOrBlank()) {
            lines.add("Error: ${simulation.errorMsg}")
        }

        if (simulation.logStatus == LogStatus.READY) {
            lines.add("")
            lines.add("The log is now available.")
            lines.add("The log will be available for 24 hours. Please download it before it expires.")
        }

        val message = SimpleMailMessage().apply {
            from = "halowplatform@gmail.com"
            setTo(recipient)
            subject = "Simulation finished: $simulationName"
            text = lines.joinToString("\n")
        }

        try {
            mailSender.send(message)
        } catch (ex: org.springframework.mail.MailException) {
            log.error("Failed to send simulation finished email for simulation {}", simulation.id, ex)
        }
    }

    fun sendLogExpiringReminder(simulation: Simulation, file: SimulationFile) {
        if (!emailEnabled) {
            log.info("Email sending disabled; skipping log expiry reminder for simulation {}", simulation.id)
            return
        }

        val recipient = getRecipientOrLog(simulation, "log expiry reminder") ?: return

        val simulationName = simulation.label?.takeIf { it.isNotBlank() }
            ?: "Simulation #${simulation.id}"

        val lines = mutableListOf(
            "Hello ${simulation.user.username},",
            "",
            "This is a reminder that the log for your simulation \"$simulationName\" will expire soon.",
            "",
            "Simulation ID: ${simulation.id}",
            "Available until: ${file.availableUntil}",
            "",
            "Please download the log before it expires."
        )

        val message = SimpleMailMessage().apply {
            from = "halowplatform@gmail.com"
            setTo(recipient)
            subject = "Reminder: download your log before it expires"
            text = lines.joinToString("\n")
        }

        try {
            mailSender.send(message)
        } catch (ex: org.springframework.mail.MailException) {
            log.error("Failed to send simulation log expiring reminder email for simulation {}", simulation.id, ex)
        }
    }

    private fun getRecipientOrLog(simulation: Simulation, emailType: String): String? {
        val recipient = simulation.user.email?.trim()?.takeIf { it.isNotBlank() }

        if (recipient == null) {
            log.info(
                "Skipping {} for simulation {} because user {} has no email",
                emailType,
                simulation.id,
                simulation.user.username
            )
        }

        return recipient
    }
}