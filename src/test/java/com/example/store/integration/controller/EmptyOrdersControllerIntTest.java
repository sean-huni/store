package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.auth.req.AuthRespDTO;
import com.example.store.dto.auth.resp.AuthReqDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
import com.example.store.persistence.repo.ProductRepo;
import com.example.store.persistence.repo.UserRepo;
import com.example.store.service.store.OrderService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApplication.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - Empty Orders")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@Import(IntTestConfig.class)
class EmptyOrdersControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ProductRepo productRepo;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clear the cache first
        orderService.clearOrdersCache();
        
        // Delete all existing data
        orderRepo.deleteAll();
        customerRepo.deleteAll();
        productRepo.deleteAll();
        userRepo.deleteAll();
        
        // Reset sequences to avoid duplicate key violations
        entityManager.createNativeQuery("ALTER SEQUENCE customer_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE order_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE product_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE user_id_seq RESTART WITH 1").executeUpdate();
        
        // Flush changes to the database
        entityManager.flush();
        entityManager.clear();
        
        // Create test user
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(User.Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        testUser = userRepo.save(testUser);
        
        // Authenticate and get token
        AuthReqDTO authRequest = new AuthReqDTO();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password");
        
        MvcResult result = mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        AuthRespDTO authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthRespDTO.class);
        
        authToken = "Bearer " + authResponse.getAccessToken();
    }

    @Test
    @DisplayName("Then return empty list when no orders exist")
    void thenReturnEmptyListWhenNoOrdersExist() throws Exception {
        // When & Then
        mockMvc.perform(get("/orders")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}