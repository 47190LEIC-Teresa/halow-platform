package backend.controller

import backend.model.dto.SimulationConfigResponse
import backend.model.dto.SimulationResponse
import backend.model.enums.LogStatus
import backend.model.enums.MetricsStatus
import backend.model.enums.SimulationStatus
import backend.security.jwt.JwtUtil
import backend.security.authorization.SimulationSecurity
import backend.service.SimulationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SimulationController::class)
@AutoConfigureMockMvc
class SimulationControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var simulationService: SimulationService

    @MockBean
    lateinit var jwtUtil: JwtUtil

    @MockBean
    lateinit var simulationSecurity: SimulationSecurity

    private fun simulationResponse(id: Long, owner: String = "testuser") = SimulationResponse(
        simulationId = id,
        status = SimulationStatus.CREATED,
        logStatus = LogStatus.NOT_READY,
        label = "test-sim",
        owner = owner,
        createdAt = "2026-06-24T20:00:00",
        startedAt = null,
        finishedAt = null,
        errorMsg = null,
        parentSimulationId = null,
        metricsStatus = MetricsStatus.NOT_REQUESTED,
        metricsErrorMsg = null
    )

    @Test
    fun `get all simulations should return 200`() {
        whenever(simulationService.getAllSimulations()).thenReturn(
            listOf(
                simulationResponse(1L, "testuser"),
                simulationResponse(2L, "testuser"),
                simulationResponse(3L, "otheruser")
            )
        )

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        mockMvc.perform(
            get("/api/simulations")
                .with(authentication(auth))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].simulationId").value(1))
            .andExpect(jsonPath("$[1].simulationId").value(2))

        verify(simulationService).getAllSimulations()
    }

    @Test
    fun `create simulation should return 202`() {
        val expectedResponse = simulationResponse(10L)

        whenever(
            simulationService.submitSimulation(
                eq("testuser"),
                any(),
                eq(true),
                isNull(),
                eq("idea-sim"),
                isNull()
            )
        ).thenReturn(expectedResponse)

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        val requestPart = MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            """
        {
          "n": 10,
          "g": 2,
          "h": 100,
          "w": 100,
          "seed": 123,
          "verbosity": 1,
          "simLength": 1000,
          "packetRate": 5,
          "slotLength": 50,
          "zippedOutput": false,
          "pE": null,
          "pP": null,
          "mp": null,
          "runSimParser": true,
          "label": "idea-sim"
        }
        """.trimIndent().toByteArray()
        )

        mockMvc.perform(
            multipart("/api/simulations")
                .file(requestPart)
                .with(authentication(auth))
                .with(csrf())
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.simulationId").value(10))
            .andExpect(jsonPath("$.owner").value("testuser"))

        verify(simulationService).submitSimulation(
            eq("testuser"),
            any(),
            eq(true),
            isNull(),
            eq("idea-sim"),
            isNull()
        )
    }

    @Test
    fun `get simulation by id should return 200`() {
        whenever(simulationSecurity.isOwner(10L, "testuser")).thenReturn(true)
        whenever(simulationService.getSimulationById(10L))
            .thenReturn(simulationResponse(10L))

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        mockMvc.perform(
            get("/api/simulations/10")
                .with(authentication(auth))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.simulationId").value(10))

        verify(simulationService).getSimulationById(10L)
        // remove: verify(simulationSecurity).isOwner(10L, "testuser")
    }

    @Test
    fun `get simulation config should return 200`() {
        whenever(simulationSecurity.isOwner(10L, "testuser")).thenReturn(true)
        whenever(simulationService.getSimulationConfigBySimulationId(10L))
            .thenReturn(
                SimulationConfigResponse(
                    seed = 123,
                    verbosity = 1,
                    stations = 10,
                    groups = 2,
                    simLength = 1000,
                    packetRate = 5,
                    slotLength = 50,
                    label = "idea-sim",
                    height = 100,
                    width = 100
                )
            )

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        mockMvc.perform(
            get("/api/simulations/10/config")
                .with(authentication(auth))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seed").value(123))
            .andExpect(jsonPath("$.stations").value(10))
            .andExpect(jsonPath("$.groups").value(2))

        verify(simulationService).getSimulationConfigBySimulationId(10L)
    }

    @Test
    fun `rerun simulation should return 202`() {
        whenever(simulationSecurity.isOwner(10L, "testuser")).thenReturn(true)
        whenever(simulationService.rerunSimulation(10L, "testuser"))
            .thenReturn(simulationResponse(11L))

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        mockMvc.perform(
            post("/api/simulations/10/rerun")
                .with(authentication(auth))
                .with(csrf())
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.simulationId").value(11))

        verify(simulationService).rerunSimulation(10L, "testuser")
    }
}