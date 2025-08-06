package com.example.store.controller.auth;

import com.example.store.dto.auth.AuthReqDTO;
import com.example.store.dto.auth.AuthRespDTO;
import com.example.store.dto.auth.RefreshTokenReqDTO;
import com.example.store.dto.auth.RegReqDTO;
import com.example.store.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private AuthRespDTO authRespDTO;

    @BeforeEach
    void setUp() {
        // Setup common test data
        authRespDTO = AuthRespDTO.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Test
    @DisplayName("Should register a new user")
    void shouldRegisterNewUser() throws Exception {
        // Given
        RegReqDTO regReqDTO = new RegReqDTO();
        regReqDTO.setFirstName("John");
        regReqDTO.setLastName("Doe");
        regReqDTO.setEmail("john.doe@example.com");
        regReqDTO.setPassword("password123");

        when(authService.register(any(RegReqDTO.class))).thenReturn(authRespDTO);

        // When/Then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("Should authenticate a user")
    void shouldAuthenticateUser() throws Exception {
        // Given
        AuthReqDTO authReqDTO = new AuthReqDTO();
        authReqDTO.setEmail("john.doe@example.com");
        authReqDTO.setPassword("password123");

        when(authService.authenticate(any(AuthReqDTO.class))).thenReturn(authRespDTO);

        // When/Then
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("Should refresh token")
    void shouldRefreshToken() throws Exception {
        // Given
        RefreshTokenReqDTO refreshTokenReqDTO = new RefreshTokenReqDTO("test-refresh-token");

        when(authService.refreshToken(any(RefreshTokenReqDTO.class))).thenReturn(authRespDTO);

        // When/Then
        mockMvc.perform(post("/auth/refresh-token")
                .header("Authorization", "Bearer test-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }
}