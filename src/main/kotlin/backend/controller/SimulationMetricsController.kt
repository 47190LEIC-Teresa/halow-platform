package backend.controller

import backend.service.SimulationMetricsService
import backend.model.dto.SimulationMetricsResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api")
class SimulationMetricsController(
    private val simulationMetricsService: SimulationMetricsService,
) {

    @PreAuthorize("@simulationSecurity.isOwner(#simulationID, authentication.name)")
    @GetMapping("/simulations/{simulationID}/metrics")
    fun getMetrics(
        @PathVariable simulationID: Long,
    ): SimulationMetricsResponse {
        return simulationMetricsService.getMetrics(simulationID)
    }

    @PreAuthorize("@simulationSecurity.isOwner(#simulationID, authentication.name)")
    @PostMapping("/simulations/{simulationID}/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    fun runMetrics(
        @PathVariable simulationID: Long
    ): SimulationMetricsResponse = simulationMetricsService.runSimulation(simulationID)

    @PostMapping(
        "/metrics",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun getFileMetrics(
        @RequestParam("file") file: MultipartFile
    ): SimulationMetricsResponse {
        return simulationMetricsService.getFileMetrics(file)
    }
}