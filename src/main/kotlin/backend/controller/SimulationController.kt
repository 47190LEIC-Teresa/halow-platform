package backend.controller

import backend.backend.model.dto.CreateSimulationBatchRequest
import backend.common.exception.ForbiddenException
import backend.model.dto.CreateSimulationRequest
import backend.backend.model.dto.SimulationConfigResponse
import backend.model.dto.SimulationResponse
import backend.service.SimulationFileService
import backend.service.SimulationService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/simulations")
class SimulationController(
    private val simulationService: SimulationService,
    private val simulationFileService: SimulationFileService
) {
    // Endpoints for authenticated users (view their own simulations)
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

        val response = simulationService.submitSimulation(
            username = authentication.name,
            params = request.toSimulatorParams(),
            runSimParser = request.runSimParser,
            gFile = fileGroups?.let {
                // Save the uploaded file to a temporary location
                val tempFile = kotlin.io.path.createTempFile(
                    prefix = "groups_file",
                    suffix = ".txt"
                ).toFile()
                it.transferTo(tempFile)
                tempFile.deleteOnExit()
                tempFile
            },
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

        val tempFile = fileGroups?.let {
            val file = kotlin.io.path.createTempFile(
                prefix = "groups_file",
                suffix = ".txt"
            ).toFile()
            it.transferTo(file)
            file
        }

        val responses = simulationService.submitSimulationBatch(
            username = authentication.name,
            request = request,
            gFile = tempFile
        )

        return ResponseEntity.accepted().body(responses)
    }

    @GetMapping("/{id}")
    fun getSimulationByIdFromUser(
        @PathVariable id: Long,
        authentication: Authentication
    ): SimulationResponse {
        val simulation = simulationService.getSimulationById(id)

        if (simulation.owner != authentication.name)
            throw ForbiddenException("You do not have access to simulation $id")

        return simulation
    }

    @GetMapping("/{id}/config")
    fun getSimulationConfigById(
        @PathVariable id: Long
    ): SimulationConfigResponse
        = simulationService.getSimulationConfigBySimulationId(id)

    @PostMapping("/{id}/rerun")
    fun rerunSimulation(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<SimulationResponse> {
        val response = simulationService.rerunSimulation(id, authentication.name)
        return ResponseEntity.accepted().body(response)
    }

}



