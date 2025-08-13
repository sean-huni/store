package com.example.store.integration.controller;

import com.example.store.StoreApp;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Role;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.CustomerRepo;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApp.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - CustomerController Security")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@org.springframework.context.annotation.Import(IntTestConfig.class)
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Customer testCustomer;
    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing data
        customerRepo.deleteAll();
        userRepo.deleteAll();
        
        // Reset sequences
        entityManager.createNativeQuery("ALTER SEQUENCE customer_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE user_id_seq RESTART WITH 1").executeUpdate();
        entityManager.flush();
        
        // Create test user
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        testUser = userRepo.save(testUser);
        
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setUpdated(ZonedDateTime.now());
        testCustomer.setCreated(ZonedDateTime.now());
        testCustomer = customerRepo.save(testCustomer);
        
        // Authenticate and get token
        final AuthReqDTO authRequest = new AuthReqDTO("test@example.com", "password");

        final MvcResult result = mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final AuthRespDTO authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthRespDTO.class);

        authToken = "Bearer " + authResponse.accessToken();
    }

    @Nested
    @DisplayName("When accessing endpoints without authentication")
    class WhenAccessingEndpointsWithoutAuthentication {

        @Test
        @DisplayName("Then return 401 when getting all customers")
        void thenReturn401WhenGettingAllCustomers() throws Exception {
            mockMvc.perform(get("/customers"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when getting customer by ID")
        void thenReturn401WhenGettingCustomerById() throws Exception {
            mockMvc.perform(get("/customers/" + testCustomer.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when creating customer")
        void thenReturn401WhenCreatingCustomer() throws Exception {
            CustomerDTO newCustomer = new CustomerDTO();
            newCustomer.setName("New Customer");

            mockMvc.perform(post("/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCustomer)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("When accessing endpoints with authentication")
    class WhenAccessingEndpointsWithAuthentication {

        @Test
        @DisplayName("Then return 200 when getting all customers")
        void thenReturn200WhenGettingAllCustomers() throws Exception {
            mockMvc.perform(get("/customers")
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$[0].name").value(testCustomer.getName()));
        }

        @Test
        @DisplayName("Then return 200 when getting customer by ID")
        void thenReturn200WhenGettingCustomerById() throws Exception {
            mockMvc.perform(get("/customers/" + testCustomer.getId())
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$.name").value(testCustomer.getName()));
        }

        @Test
        @DisplayName("Then return 201 when creating customer")
        void thenReturn201WhenCreatingCustomer() throws Exception {
            CustomerDTO newCustomer = new CustomerDTO();
            newCustomer.setName("New Customer");

            mockMvc.perform(post("/customers")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newCustomer)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Customer"))
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }
    }
}