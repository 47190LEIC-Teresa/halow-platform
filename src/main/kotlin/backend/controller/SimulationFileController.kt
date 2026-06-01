package backend.controller

import backend.common.exception.ForbiddenException
import backend.model.dto.SimulationFileResponse
import backend.model.dto.toFileResponse
import backend.model.enums.FileType
import backend.service.SimulationFileService
import backend.service.SimulationService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Console

@RestController
@RequestMapping("/api/files")
class SimulationFileController(
    private val simulationFileService: SimulationFileService,
    private val simulationService: SimulationService
) {

    @GetMapping("/{simulationID}")
    fun getSimulationFiles(@PathVariable simulationID: Long, authentication: Authentication): List<SimulationFileResponse> {
        val simulation = simulationService.getSimulationById(simulationID)
            ?: throw IllegalArgumentException("Simulation not found: $simulationID")

        if (simulation.owner != authentication.name) {
            throw ForbiddenException("You do not have access to simulation $simulationID")
        }

        val files = simulationFileService.getAllFiles(simulationID).toMutableList()

        val hasOwnGroupsFile = files.any { it.fileType == FileType.G }

        if (!hasOwnGroupsFile) {
            println("DOES NOT HAVE OWN GROUPS FILE")
            simulation.parentSimulationId ?.let { parentId ->
                simulationFileService.getGroupsFile(parentId)?.let{
                    files.add(it)
                    println("Exists!!")
                }
            }
        }

        println("Files for simulation $simulationID: ${files.map { "${it.fileName} (${it.fileType})" }}")

        if (files.isEmpty()) {
            return emptyList()
        }

        return files.map(::toFileResponse)
    }

    @GetMapping("/{fileID}/download")
    fun downloadFile(
        @PathVariable fileID: Long,
        authentication: Authentication
    ): ResponseEntity<ByteArray> {
        val file = simulationFileService.getFileById(fileID)

        if (file.simulation.user.username != authentication.name) {
            throw ForbiddenException("You do not have access to simulation ${file.simulation.id}")
        }

        val data = file.fileData
            ?: throw IllegalArgumentException("File is no longer available")

        if (file.fileType == FileType.LOG && file.downloaded) {
            throw IllegalArgumentException("Log file has already been downloaded")
        }

        val response = ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"${file.fileName}\""
            )
            .body(data)

        if (file.fileType == FileType.LOG) {
            simulationFileService.markDownloadedAndClearLog(file)
        }

        return response
    }
}
