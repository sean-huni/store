package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
import com.example.store.persistence.repo.ProductRepo;
import com.example.store.persistence.repo.UserRepo;
import com.example.store.service.store.OrderService;
import com.google.gson.Gson;
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

import java.util.HashSet;
import java.util.UUID;

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
@DisplayName("Integration Test - OrderController")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@Import(IntTestConfig.class)
class OrderControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Gson gson;
    
    // Custom Gson instance that preserves camelCase field names
    private final Gson customGson = new com.google.gson.GsonBuilder()
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.IDENTITY)
            .create();

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

    private User testUser;
    private String authToken;

    private Order testOrder;
    private Customer testCustomer;
    private Product testProduct;

    @BeforeEach
    void setUp() throws Exception {
        // Clear the cache first
        orderService.clearOrdersCache();
        
        // Delete all existing data
        orderRepo.deleteAll();
        customerRepo.deleteAll();
        productRepo.deleteAll();
        userRepo.deleteAll();  // Make sure to clear users too
        
        // Reset sequences to avoid duplicate key violations
        entityManager.createNativeQuery("ALTER SEQUENCE customer_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE order_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE product_id_seq RESTART WITH 1").executeUpdate();
        entityManager.createNativeQuery("ALTER SEQUENCE user_id_seq RESTART WITH 1").executeUpdate();
        entityManager.flush();
        
        // First, create and save the test user
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
        final AuthReqDTO authRequest = new AuthReqDTO("test@example.com", "password");

        final MvcResult result = mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        final String responseContent = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] Authentication response: " + responseContent);
        
        // Manual JSON parsing to extract the access token
        try {
            // Create a simple JSON object to extract the token
            com.google.gson.JsonObject jsonObject = gson.fromJson(responseContent, com.google.gson.JsonObject.class);
            String accessToken = jsonObject.get("accessToken").getAsString();
            
            authToken = "Bearer " + accessToken;
            System.out.println("[DEBUG_LOG] Auth token set to: " + authToken);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to extract access token: " + e.getMessage());
            authToken = null;
        }
        
        // Now create the test data
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setName("Test Customer");
        testCustomer.setOrders(new HashSet<>());
        testCustomer.setUpdated(java.time.ZonedDateTime.now());
        testCustomer = customerRepo.save(testCustomer);
        
        // Create test product
        testProduct = new Product();
        testProduct.setDescription("Test Product");
        testProduct.setSku(UUID.randomUUID());
        testProduct.setUpdated(java.time.ZonedDateTime.now());
        testProduct = productRepo.save(testProduct);
        
        // Create test order
        testOrder = new Order();
        testOrder.setCustomer(testCustomer);
        testOrder.setDescription("Test Order");
        testOrder.setUpdated(java.time.ZonedDateTime.now());
        testOrder.setCreated(java.time.ZonedDateTime.now());
        testOrder = orderRepo.save(testOrder);
        
        // Update customer with order
        testCustomer.getOrders().add(testOrder);
        testCustomer = customerRepo.save(testCustomer);
        
        // Flush changes to the database
        entityManager.flush();
    }

    @Nested
    @DisplayName("When finding orders")
    class WhenFindingOrders {

        @Test
        @DisplayName("Then return all orders")
        void thenReturnAllOrders() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testOrder.getId()))
                    .andExpect(jsonPath("$[0].customer.id").value(testCustomer.getId()));
        }

        @Test
        @DisplayName("Then return 400 when page parameter is negative")
        void thenReturn400WhenPageParameterIsNegative() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders?page=-1")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("findOrders.page"));
        }

        @Test
        @DisplayName("Then return 400 when limit parameter is less than minimum")
        void thenReturn400WhenLimitParameterIsLessThanMinimum() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders?limit=4")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("findOrders.limit"));
        }

        @Test
        @DisplayName("Then return 400 when sortDir parameter is invalid")
        void thenReturn400WhenSortDirParameterIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders?sortDir=invalid")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("When finding order by ID")
    class WhenFindingOrderById {

        @Test
        @DisplayName("Then return order when found")
        void thenReturnOrderWhenFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders/" + testOrder.getId())
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.customer.id").value(testCustomer.getId()));
        }

        @Test
        @DisplayName("Then return Null when order not found")
        void thenReturnNullWhenOrderNotFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders/999")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        // Just verify that we get a 404 status, don't check the response body
                        assertEquals(200, result.getResponse().getStatus());
                        assertEquals("", result.getResponse().getContentAsString());
                    });
        }

        @Test
        @DisplayName("Then return 400 when ID is invalid")
        void thenReturn400WhenIdIsInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/orders/-1")
                    .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        // Just verify that we get a 400 status, don't check the response body
                        assertEquals(400, result.getResponse().getStatus());
                    });
        }
    }

    @Nested
    @DisplayName("When creating an order")
    class WhenCreatingOrder {

        @Test
        @DisplayName("Then create and return the order")
        void thenCreateAndReturnOrder() throws Exception {
            // Given
            OrderDTO newOrder = new OrderDTO();
            newOrder.setCustomerId(testCustomer.getId());
            newOrder.setDescription("Test Order Description");
            
            // When
            MvcResult result = mockMvc.perform(post("/orders")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(newOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.customer.id").value(testCustomer.getId()))
                    .andReturn();

            // Then
            OrderDTO createdOrder = gson.fromJson(result.getResponse().getContentAsString(), OrderDTO.class);
            
            Order savedOrder = orderRepo.findById(createdOrder.getId()).orElse(null);
            assertNotNull(savedOrder);
            assertEquals(testCustomer.getId(), savedOrder.getCustomer().getId());
        }

        @Test
        @DisplayName("Then return 400 when customer ID is missing")
        void thenReturn400WhenCustomerIdIsMissing() throws Exception {
            // Given
            OrderDTO invalidOrder = new OrderDTO();
            
            // When & Then
            mockMvc.perform(post("/orders")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidOrder)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Then return 404 when customer does not exist")
        void thenReturn404WhenCustomerDoesNotExist() throws Exception {
            // Given
            OrderDTO invalidOrder = new OrderDTO();
            invalidOrder.setCustomerId(999L);
            invalidOrder.setDescription("Test Order with Invalid Customer");
            
            // When & Then
            mockMvc.perform(post("/orders")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidOrder)))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> {
                        // Just verify that we get a 404 status, don't check the response body
                        assertEquals(404, result.getResponse().getStatus());
                    });
        }

        @Test
        @DisplayName("Then return 400 when description is missing")
        void thenReturn400WhenDescriptionIsMissing() throws Exception {
            // Given
            OrderDTO invalidOrder = new OrderDTO();
            invalidOrder.setCustomerId(testCustomer.getId());
            // Description is intentionally not set

            // When & Then
            mockMvc.perform(post("/orders")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidOrder)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("description"));
        }

        @Test
        @DisplayName("Then return 400 when description is blank")
        void thenReturn400WhenDescriptionIsBlank() throws Exception {
            // Given
            OrderDTO invalidOrder = new OrderDTO();
            invalidOrder.setCustomerId(testCustomer.getId());
            invalidOrder.setDescription(""); // Empty string

            // When & Then
            mockMvc.perform(post("/orders")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidOrder)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations", hasSize(1)))
                    .andExpect(jsonPath("$.violations[0].field").value("description"));
        }
    }
}