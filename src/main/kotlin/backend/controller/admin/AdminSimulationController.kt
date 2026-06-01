package backend.controller.admin

import backend.model.dto.CreateSimulationRequest
import backend.model.dto.SimulationResponse
import backend.service.SimulationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/admin/simulations")
class AdminSimulationController(
    private val simulationService: SimulationService,
) {

    @GetMapping
    fun getAllSimulations(): List<SimulationResponse> {
        return simulationService.getAllSimulations()
    }

    @GetMapping("/{id}")
    fun getSimulation(@PathVariable id: Long): SimulationResponse
        = simulationService.getSimulationById(id)


    @PostMapping("/{username}")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSimulation(
        @RequestBody request: CreateSimulationRequest,
        @PathVariable username: String
    ): SimulationResponse {
        return simulationService.submitSimulation(
            username = username,
            params = request.toSimulatorParams(),
            runSimParser = request.runSimParser
        )
    }

    @GetMapping("/{username}")
    fun getAllSimulationsFromUser( @PathVariable username: String): List<SimulationResponse> {
        return simulationService.getAllSimulations().filter { it.owner == username }
    }


}



