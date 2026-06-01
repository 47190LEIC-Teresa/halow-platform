package backend.controller

import backend.model.dto.UserResponse
import backend.security.jwt.JwtUtil
import backend.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [UserController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var userService: UserService

    @MockBean
    lateinit var jwtUtil: JwtUtil

    @Test
    fun `get user should return 200 for authenticated user`() {
        whenever(userService.getUserByUsername("testuser")).thenReturn(
            UserResponse(
                username = "testuser",
                email = "testuser@example.com",
                firstName = "Test",
                lastName = "User"
            )
        )

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())

        mockMvc.perform(
            get("/api/user")
                .principal(auth)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("testuser@example.com"))

        verify(userService).getUserByUsername("testuser")
    }

    @Test
    fun `update user should return 200 for authenticated user`() {
        whenever(userService.updateUser(eq("testuser"), any())).thenReturn(
            UserResponse(
                username = "testuser",
                email = "testuser@example.com",
                firstName = "Updated",
                lastName = "User"
            )
        )

        val auth = UsernamePasswordAuthenticationToken("testuser", null, emptyList())
        val body = mapOf(
            "firstName" to "Updated",
            "lastName" to "User"
        )

        mockMvc.perform(
            patch("/api/user")
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.firstName").value("Updated"))

        verify(userService).updateUser(eq("testuser"), any())
    }
}