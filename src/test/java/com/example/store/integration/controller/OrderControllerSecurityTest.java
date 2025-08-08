package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
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
import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApplication.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - OrderController Security")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@org.springframework.context.annotation.Import(IntTestConfig.class)
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Order testOrder;
    private Customer testCustomer;
    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing data
        orderRepo.deleteAll();
        customerRepo.deleteAll();
        userRepo.deleteAll();
        
        // Reset sequences
        entityManager.createNativeQuery("ALTER SEQUENCE order_id_seq RESTART WITH 1").executeUpdate();
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
        
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setUpdated(ZonedDateTime.now());
        testCustomer.setCreated(ZonedDateTime.now());
        testCustomer = customerRepo.save(testCustomer);
        
        // Create test order
        testOrder = new Order();
        testOrder.setCustomer(testCustomer);
        testOrder.setDescription("Test Order Description");
        testOrder.setProducts(new ArrayList<>());
        testOrder.setUpdated(ZonedDateTime.now());
        testOrder.setCreated(ZonedDateTime.now());
        testOrder = orderRepo.save(testOrder);
        
        // Authenticate and get token
        final AuthReqDTO authRequest = new AuthReqDTO("test@example.com", "password");

        final MvcResult result = mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final AuthRespDTO authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthRespDTO.class);

        authToken = "Bearer %s".formatted(authResponse.accessToken());
    }

    @Nested
    @DisplayName("When accessing endpoints without authentication")
    class WhenAccessingEndpointsWithoutAuthentication {

        @Test
        @DisplayName("Then return 401 when getting all orders")
        void thenReturn401WhenGettingAllOrders() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when getting order by ID")
        void thenReturn401WhenGettingOrderById() throws Exception {
            mockMvc.perform(get("/orders/" + testOrder.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when creating order")
        void thenReturn401WhenCreatingOrder() throws Exception {
            OrderDTO newOrder = new OrderDTO();
            newOrder.setCustomerId(testCustomer.getId());

            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newOrder)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("When accessing endpoints with authentication")
    class WhenAccessingEndpointsWithAuthentication {

        @Test
        @DisplayName("Then return 200 when getting all orders")
        void thenReturn200WhenGettingAllOrders() throws Exception {
            mockMvc.perform(get("/orders")
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testOrder.getId()));
        }

        @Test
        @DisplayName("Then return 200 when getting order by ID")
        void thenReturn200WhenGettingOrderById() throws Exception {
            mockMvc.perform(get("/orders/" + testOrder.getId())
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testOrder.getId()));
        }

        @Test
        @DisplayName("Then return 201 when creating order")
        void thenReturn201WhenCreatingOrder() throws Exception {
            OrderDTO newOrder = new OrderDTO();
            newOrder.setCustomerId(testCustomer.getId());
            newOrder.setDescription("New Test Order Description");

            mockMvc.perform(post("/orders")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.customerId").value(testCustomer.getId()))
                    .andExpect(jsonPath("$.description").value("New Test Order Description"));
        }
    }
}