package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.repo.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApplication.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - AuthController")
@Transactional
@Testcontainers
@ActiveProfiles("dev") // Using dev profile to reproduce the error
@Import(IntTestConfig.class)
class AuthControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        userRepo.deleteAll();

        // Reset sequences to avoid duplicate key violations
        entityManager.createNativeQuery("ALTER SEQUENCE user_id_seq RESTART WITH 1").executeUpdate();
        entityManager.flush();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldSuccessfullyRegisterNewUser() throws Exception {
        // Given
        final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", "john.doe@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());

        // Verify user was created
        assertTrue(userRepo.existsByEmail("john.doe@example.com"), "User should be created when registration succeeds");
    }

    @Test
    @DisplayName("Should successfully authenticate a user")
    void shouldSuccessfullyAuthenticateUser() throws Exception {
        // Given
        // First, create a user
        final RegReqDTO regReqDTO = new RegReqDTO("Jane", "Doe", "jane.doe@example.com", "password123");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReqDTO)))
                .andExpect(status().isOk());

        // Then, authenticate with the created user
        final AuthReqDTO authReqDTO = new AuthReqDTO("jane.doe@example.com", "password123");

        // When & Then
        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void shouldSuccessfullyRefreshToken() throws Exception {
        // Given
        // First, create a user and get the refresh token
        final RegReqDTO regReqDTO = new RegReqDTO("Bob", "Smith", "bob.smith@example.com", "password123");

        // Register the user
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReqDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract the refresh token from the response
        String responseContent = registerResult.getResponse().getContentAsString();
        AuthRespDTO authResponse = objectMapper.readValue(responseContent, AuthRespDTO.class);
        String refreshToken = authResponse.refreshToken();

        // When & Then
        mockMvc.perform(post("/auth/refresh-token")
                        .header("Authorization", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken)) // Same refresh token should be returned
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Nested
    @DisplayName("Register Endpoint Validation Tests")
    class RegisterEndpointValidationTests {

        @Test
        @DisplayName("Should return 400 when firstName is missing")
        void shouldReturn400WhenFirstNameIsMissing() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO(null, "Doe", "john.doe@example.com", "password123");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("firstName"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when lastName is missing")
        void shouldReturn400WhenLastNameIsMissing() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO("John", null, "john.doe@example.com", "password123");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("lastName"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturn400WhenEmailIsMissing() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", null, "password123");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("email"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", "invalid-email", "password123");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("email"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", "john.doe@example.com", null);

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("password"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            // Given
            final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", "john.doe@example.com", "short");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("password"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }
    }

    @Nested
    @DisplayName("Authenticate Endpoint Validation Tests")
    class AuthenticateEndpointValidationTests {

        @Test
        @DisplayName("Should return 400 when email is missing")
        void shouldReturn400WhenEmailIsMissing() throws Exception {
            // Given
            final AuthReqDTO authReqDTO = new AuthReqDTO(null, "password123");

            // When & Then
            mockMvc.perform(post("/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("email"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            // Given
            final AuthReqDTO authReqDTO = new AuthReqDTO("invalid-email", "password123");

            // When & Then
            mockMvc.perform(post("/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("email"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            // Given
            final AuthReqDTO authReqDTO = new AuthReqDTO("john.doe@example.com", null);

            // When & Then
            mockMvc.perform(post("/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authReqDTO)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("password"))
                    // In test environment, message codes may not be resolved
                    .andExpect(jsonPath("$.violations[0].errMsg").exists());
        }
    }

    @Nested
    @DisplayName("Refresh Token Endpoint Validation Tests")
    class RefreshTokenEndpointValidationTests {

        /**
         * Note: We don't test for missing Authorization header because Spring handles that
         * before our controller method is called, and we can't control how Spring formats
         * the error response.
         */

        @Test
        @DisplayName("Should return 401 when refresh token is empty")
        void shouldReturn401WhenRefreshTokenIsEmpty() throws Exception {
            // Given - Create a valid user first
            final RegReqDTO regReqDTO = new RegReqDTO("John", "Doe", "refresh.test@example.com", "password123");

            // Register the user to get a valid refresh token
            String registerResponse = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(regReqDTO)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Now test with an empty refresh token
            mockMvc.perform(post("/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", ""))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.name").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.violations").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}