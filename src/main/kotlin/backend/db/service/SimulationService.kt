package backend.db.service

import backend.db.entities.Simulation
import backend.db.entities.SimulationConfig
import backend.db.enums.FileType
import backend.db.enums.LogStatus
import backend.db.enums.SimulationStatus
import backend.db.repositories.AppUserRepository
import backend.db.repositories.SimulationConfigRepository
import backend.db.repositories.SimulationFileRepository
import backend.db.repositories.SimulationRepository
import backend.simulator.HalowSimulatorRunner
import backend.simulator.SimulationRunner
import backend.simulator.SimulatorParams
import backend.storage.SimulationOutputStorage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDateTime

@Service
class SimulationService(
    private val appUserRepository: AppUserRepository,
    private val simulationConfigRepository: SimulationConfigRepository,
    private val simulationRepository: SimulationRepository,
    private val simulationFileRepository: SimulationFileRepository,
    private val simulationRunner: SimulationRunner = HalowSimulatorRunner(),
    private val storage: SimulationOutputStorage = SimulationOutputStorage(),
) {

    @Transactional
    fun runSimulation(username: String, params: SimulatorParams) {
        val user = appUserRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")

        val existingConfig = simulationConfigRepository.findMatchingConfig(
            params.g,
            params.n,
            params.w,
            params.h,
            params.verbosity,
            params.simLength,
            params.packetRate,
            params.slotLength
        )

        // if a matching config exists, reuse it; otherwise, create a new one
        val config = existingConfig ?: simulationConfigRepository.save(
            SimulationConfig(
                nGroups = params.g,
                nStations = params.n,
                width = params.w,
                height = params.h,
                verbosity = params.verbosity,
                simLength = params.simLength,
                packetRate = params.packetRate,
                slotLength = params.slotLength
            )
        )

        val simulation = simulationRepository.save(
            Simulation(
                user = user,
                config = config,
                status = SimulationStatus.CREATED,
                logStatus = LogStatus.NOT_READY,
                createdAt = LocalDateTime.now(),
                seed = params.seed,
                wMp = !params.mp.isNullOrBlank(),
                wPp = !params.pP.isNullOrBlank(),
                wPe = !params.pE.isNullOrBlank(),
                wGroupFile = !params.fileGroups.isNullOrBlank(),
                zippedOutput = params.zippedOutput
            )
        )

        val simulationDir = storage.createSimulationDirectory(simulation.id!!)

        simulation.status = SimulationStatus.RUNNING
        simulation.startedAt = LocalDateTime.now()
        simulationRepository.save(simulation)

        val result = simulationRunner.run(params, simulationDir)

        simulation.finishedAt = LocalDateTime.now()
        if (result.exitCode == 0) {
            simulation.status = SimulationStatus.COMPLETED
            simulation.logStatus = LogStatus.READY
        } else {
            simulation.status = SimulationStatus.FAILED
        }

        simulationRepository.save(simulation)

        fun saveFileIfExists(
            fileType: FileType,
            fileName: String? = null,
            finder: (() -> File?)? = null
        ) {
            val file = when {
                finder != null -> finder()
                !fileName.isNullOrBlank() -> File(simulationDir, fileName)
                else -> null
            } ?: return

            if (!file.exists()) {
                throw IllegalArgumentException("$fileType file not found: ${file.name}")
            }

            val entity = storage.buildSimulationFile(
                simulation = simulation,
                file = file,
                fileType = fileType
            )
            simulationFileRepository.save(entity)
        }

        saveFileIfExists(FileType.LOG, finder = { File(simulationDir, "log.txt") })
        saveFileIfExists(FileType.MP, params.mp)
        saveFileIfExists(FileType.PP, params.pP)
        saveFileIfExists(FileType.PE, params.pE)

        if (params.zippedOutput) {
            saveFileIfExists(
                FileType.ZIPPED,
                finder = {
                    simulationDir.listFiles()
                        ?.firstOrNull { it.extension.equals("zip", ignoreCase = true) }
                }
            )
        }
    }
}