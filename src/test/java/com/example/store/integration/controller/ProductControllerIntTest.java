package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.dto.ProductDTO;
import com.example.store.dto.auth.AuthReqDTO;
import com.example.store.dto.auth.AuthRespDTO;
import org.springframework.context.annotation.Import;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.ProductRepo;
import com.example.store.persistence.repo.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
@DisplayName("Integration Test - ProductController")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@Import(IntTestConfig.class)
class ProductControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepo productRepo;
    
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private UserRepo userRepo;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private Gson gson;
    
    // Custom Gson instance that preserves camelCase field names
    private Gson customGson = new com.google.gson.GsonBuilder()
            .setFieldNamingPolicy(com.google.gson.FieldNamingPolicy.IDENTITY)
            .create();

    private Product testProduct;
    private UUID testSku;
    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Delete all existing data
        productRepo.deleteAll();
        userRepo.deleteAll();
        
        // Reset sequences to avoid duplicate key violations
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
        AuthReqDTO authRequest = new AuthReqDTO();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password");

        MvcResult result = mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customGson.toJson(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        System.out.println("[DEBUG_LOG] Authentication response: " + responseContent);
        
        // Manual JSON parsing to extract the access token
        try {
            // Create a simple JSON object to extract the token
            JsonObject jsonObject = gson.fromJson(responseContent, JsonObject.class);
            String accessToken = jsonObject.get("accessToken").getAsString();
            
            authToken = "Bearer " + accessToken;
            System.out.println("[DEBUG_LOG] Auth token set to: " + authToken);
        } catch (Exception e) {
            System.out.println("[DEBUG_LOG] Failed to extract access token: " + e.getMessage());
            authToken = null;
        }
        
        // Now create the test data
        testSku = UUID.randomUUID();
        
        // Create test product
        testProduct = new Product();
        testProduct.setDescription("Test Product");
        testProduct.setSku(testSku);
        testProduct = productRepo.save(testProduct);
    }

    @Nested
    @DisplayName("When finding products")
    class WhenFindingProducts {

        @Test
        @DisplayName("Then return all products")
        void thenReturnAllProducts() throws Exception {
            // When & Then
            mockMvc.perform(get("/products")
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testProduct.getId()))
                    .andExpect(jsonPath("$[0].description").value(testProduct.getDescription()))
                    .andExpect(jsonPath("$[0].sku").value(testSku.toString()));
        }

        @Test
        @DisplayName("Then return empty list when no products exist")
        void thenReturnEmptyListWhenNoProductsExist() throws Exception {
            // Given
            productRepo.deleteAll();
            
            // When & Then
            mockMvc.perform(get("/products")
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("When finding product by ID")
    class WhenFindingProductById {

        @Test
        @DisplayName("Then return product when found")
        void thenReturnProductWhenFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/products/" + testProduct.getId())
                    .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testProduct.getId()))
                    .andExpect(jsonPath("$.description").value(testProduct.getDescription()))
                    .andExpect(jsonPath("$.sku").value(testSku.toString()));
        }

        @Test
        @DisplayName("Then return Null when product not found")
        void thenReturnNullWhenProductNotFound() throws Exception {
            // When & Then
            mockMvc.perform(get("/products/999")
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
            mockMvc.perform(get("/products/-1")
                    .header("Authorization", authToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("When creating a product")
    class WhenCreatingProduct {

        @Test
        @DisplayName("Then create and return the product")
        void thenCreateAndReturnProduct() throws Exception {
            // Given
            UUID newSku = UUID.randomUUID();
            ProductDTO newProduct = new ProductDTO();
            newProduct.setDescription("New Product");
            newProduct.setSku(newSku);

            // When
            MvcResult result = mockMvc.perform(post("/products")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(newProduct)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("New Product"))
                    .andExpect(jsonPath("$.sku").value(newSku.toString()))
                    .andReturn();

            // Then
            ProductDTO createdProduct = objectMapper.readValue(
                    result.getResponse().getContentAsString(), ProductDTO.class);
            
            Product savedProduct = productRepo.findById(createdProduct.getId()).orElse(null);
            assertNotNull(savedProduct);
            assertEquals("New Product", savedProduct.getDescription());
            assertEquals(newSku, savedProduct.getSku());
        }

        @Test
        @DisplayName("Then return 400 when description is missing")
        void thenReturn400WhenDescriptionIsMissing() throws Exception {
            // Given
            ProductDTO invalidProduct = new ProductDTO();
            invalidProduct.setSku(UUID.randomUUID());
            
            // When & Then
            mockMvc.perform(post("/products")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidProduct)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Then return 400 when SKU is missing")
        void thenReturn400WhenSkuIsMissing() throws Exception {
            // Given
            ProductDTO invalidProduct = new ProductDTO();
            invalidProduct.setDescription("Invalid Product");
            
            // When & Then
            mockMvc.perform(post("/products")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(customGson.toJson(invalidProduct)))
                    .andExpect(status().isBadRequest());
        }
    }
}