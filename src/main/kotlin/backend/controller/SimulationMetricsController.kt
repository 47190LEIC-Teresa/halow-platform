package backend.backend.controller

import backend.backend.service.SimulationMetricsService
import backend.model.dto.SimulationMetricsResponse
import backend.service.SimulationFileService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api")
class SimulationMetricsController(
    private val simulationMetricsService: SimulationMetricsService,
) {
    @GetMapping("/simulations/{simulationID}/metrics")
    fun getMetrics(
        @PathVariable simulationID: Long,
        //authentication: Authentication TODO: should we check ownership here?
    ): ResponseEntity<SimulationMetricsResponse> {
        return simulationMetricsService.getMetrics(simulationID)
    }

    @PostMapping("/simulations/{simulationID}/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    fun runMetrics(
        @PathVariable simulationID: Long
        //authentication: Authentication TODO: should we check ownership here?
    ): SimulationMetricsResponse = simulationMetricsService.runSimulation(simulationID)

    @PostMapping(
        "/metrics",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun getFileMetrics(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<SimulationMetricsResponse> {
        return ResponseEntity.ok(simulationMetricsService.getFileMetrics(file))
    }
}