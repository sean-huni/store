package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Customer;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApplication.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - CustomerController")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@Import(IntTestConfig.class)
class CustomerControllerIntTest {

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

    private Customer testCustomer;
    private User testUser;
    private String authToken;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing data
        customerRepo.deleteAll();
        userRepo.deleteAll();

        // Reset sequences to avoid duplicate key violations
        entityManager.createNativeQuery("ALTER SEQUENCE customer_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE user_id_seq RESTART WITH 1").executeUpdate();
        entityManager.flush();

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

        // Create a new customer for each test
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

        final AuthRespDTO authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthRespDTO.class);

        authToken = "Bearer " + authResponse.accessToken();
    }

    @Nested
    @DisplayName("When finding customers")
    class WhenFindingCustomers {

        @Test
        @DisplayName("Then return all customers when no filters applied")
        void thenReturnAllCustomersWhenNoFiltersApplied() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$[0].name").value(testCustomer.getName()));
        }

        @Test
        @DisplayName("Then return filtered customers when name filter applied")
        void thenReturnFilteredCustomersWhenNameFilterApplied() throws Exception {
            // Given
            Customer anotherCustomer = new Customer();
            anotherCustomer.setName("Another Customer");
            anotherCustomer.setUpdated(ZonedDateTime.now());
            anotherCustomer.setCreated(ZonedDateTime.now());
            customerRepo.save(anotherCustomer);

            // When & Then
            mockMvc.perform(get("/customers?name=Test")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$[0].name").value(testCustomer.getName()));
        }

        @Test
        @DisplayName("Then return empty list when no customers match filter")
        void thenReturnEmptyListWhenNoCustomersMatchFilter() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers?name=NonExistent")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Then return 400 when page parameter is negative")
        void thenReturn400WhenPageParameterIsNegative() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers?page=-1")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("findCustomers.page"));
        }

        @Test
        @DisplayName("Then return 400 when limit parameter is less than minimum")
        void thenReturn400WhenLimitParameterIsLessThanMinimum() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers?limit=4")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("findCustomers.limit"));
        }

        @Test
        @DisplayName("Then return 400 when sortDir parameter is invalid")
        void thenReturn400WhenSortDirParameterIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers?sortDir=invalid")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("When finding customer by ID")
    class WhenFindingCustomerById {

        @Test
        @DisplayName("Then return customer when found")
        void thenReturnCustomerWhenFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers/" + testCustomer.getId())
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCustomer.getId()))
                    .andExpect(jsonPath("$.name").value(testCustomer.getName()));
        }

        @Test
        @DisplayName("Then return NULL when customer not found")
        void thenReturnNullWhenCustomerNotFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers/999")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        assertEquals(200, result.getResponse().getStatus());
                        assertEquals("", result.getResponse().getContentAsString());
                    });
        }

        @Test
        @DisplayName("Then return 400 when ID is invalid")
        void thenReturn400WhenIdIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/customers/-1")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        // Just verify that we get a 400 status, don't check the response body
                        assertEquals(400, result.getResponse().getStatus());
                    });
        }
    }

    @Nested
    @DisplayName("When creating a customer")
    class WhenCreatingCustomer {

        @Test
        @DisplayName("Then create and return the customer")
        void thenCreateAndReturnCustomer() throws Exception {
            // Given
            CustomerDTO newCustomer = new CustomerDTO();
            newCustomer.setName("New Customer");

            // When
            MvcResult result = mockMvc.perform(post("/customers")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCustomer)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("New Customer"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andReturn();

            // Then
            CustomerDTO createdCustomer = objectMapper.readValue(
                    result.getResponse().getContentAsString(), CustomerDTO.class);

            Customer savedCustomer = customerRepo.findById(createdCustomer.getId()).orElse(null);
            assertNotNull(savedCustomer);
            assertEquals("New Customer", savedCustomer.getName());
        }

        @Test
        @DisplayName("Then return 400 when name is missing")
        void thenReturn400WhenNameIsMissing() throws Exception {
            // Given
            CustomerDTO invalidCustomer = new CustomerDTO();

            // When & Then
            mockMvc.perform(post("/customers")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCustomer)))
                    .andExpect(status().isBadRequest());
        }
    }
}