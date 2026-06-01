package backend.controller

import backend.model.dto.LoginResponse
import backend.security.jwt.JwtUtil
import backend.service.AuthService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var authService: AuthService

    @MockBean
    lateinit var jwtUtil: JwtUtil

    @Test
    fun `register should return 201 and token`() {
        whenever(authService.register(any()))
            .thenReturn(LoginResponse("jwt-token"))

        val body = mapOf(
            "username" to "testuser",
            "email" to "testuser@example.com",
            "password" to "Test123!",
            "firstName" to "Test",
            "lastName" to "User"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").value("jwt-token"))

        verify(authService).register(any())
    }

    @Test
    fun `login should return 200 and token`() {
        whenever(authService.login(any()))
            .thenReturn(LoginResponse("jwt-token"))

        val body = mapOf(
            "identifier" to "testuser",
            "password" to "Test123!"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("jwt-token"))

        verify(authService).login(any())
    }
}