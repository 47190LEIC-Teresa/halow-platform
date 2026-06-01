package backend.controller

import backend.model.dto.CreateSimulationBatchRequest
import backend.model.dto.CreateSimulationRequest
import backend.model.dto.SimulationConfigResponse
import backend.model.dto.SimulationResponse
import backend.service.SimulationFileService
import backend.service.SimulationService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import kotlin.io.path.createTempFile
import java.io.File


@RestController
@RequestMapping("/api/simulations")
class SimulationController(
    private val simulationService: SimulationService,
) {
    @GetMapping
    fun getAllSimulationsFromUser(authentication: Authentication): List<SimulationResponse> {
        return simulationService.getAllSimulations().filter { it.owner == authentication.name }
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createSimulationForUser(
        @RequestPart("request") request: CreateSimulationRequest,
        @RequestPart("fileGroups", required = false) fileGroups: MultipartFile?,
        authentication: Authentication
    ): ResponseEntity<SimulationResponse> {
        val tempFile = fileGroups?.toTempFile("groups_file", ".txt")

        val response = simulationService.submitSimulation(
            username = authentication.name,
            params = request.toSimulatorParams(),
            runSimParser = request.runSimParser,
            gFile = tempFile,
            label = request.label
        )

        return ResponseEntity.accepted().body(response)
    }

    @PostMapping(path = ["/batch"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createSimulationBatchForUser(
        @RequestPart("request") request: CreateSimulationBatchRequest,
        @RequestPart("fileGroups", required = false) fileGroups: MultipartFile?,
        authentication: Authentication
    ): ResponseEntity<List<SimulationResponse>> {
        val tempFile = fileGroups?.toTempFile("groups_file", ".txt")

        val responses = simulationService.submitSimulationBatch(
            username = authentication.name,
            request = request,
            gFile = tempFile
        )

        return ResponseEntity.accepted().body(responses)
    }

    @PreAuthorize("@simulationSecurity.isOwner(#id, authentication.name)")
    @GetMapping("/{id}")
    fun getSimulationByIdFromUser(
        @PathVariable id: Long
    ): SimulationResponse {
        return simulationService.getSimulationById(id)
    }

    @PreAuthorize("@simulationSecurity.isOwner(#id, authentication.name)")
    @GetMapping("/{id}/config")
    fun getSimulationConfigById(
        @PathVariable id: Long
    ): SimulationConfigResponse {
        return simulationService.getSimulationConfigBySimulationId(id)
    }

    @PreAuthorize("@simulationSecurity.isOwner(#id, authentication.name)")
    @PostMapping("/{id}/rerun")
    fun rerunSimulation(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<SimulationResponse> {
        val response = simulationService.rerunSimulation(id, authentication.name)
        return ResponseEntity.accepted().body(response)
    }

    private fun MultipartFile.toTempFile(prefix: String, suffix: String): File {
        val path = createTempFile(prefix = prefix, suffix = suffix)
        val file = path.toFile()
        transferTo(file)
        file.deleteOnExit()
        return file
    }
}



