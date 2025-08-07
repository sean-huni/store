package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.auth.resp.RegReqDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.repo.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        RegReqDTO regReqDTO = new RegReqDTO();
        regReqDTO.setFirstName("John");
        regReqDTO.setLastName("Doe");
        regReqDTO.setEmail("john.doe@example.com");
        regReqDTO.setPassword("password123");

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
}