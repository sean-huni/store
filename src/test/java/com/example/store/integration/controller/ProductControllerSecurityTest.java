package com.example.store.integration.controller;

import com.example.store.StoreApp;
import com.example.store.dto.ProductDTO;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.Role;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.ProductRepo;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApp.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - ProductController Security")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@org.springframework.context.annotation.Import(IntTestConfig.class)
class ProductControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Product testProduct;
    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up existing data
        productRepo.deleteAll();
        userRepo.deleteAll();

        // Reset sequences
        entityManager.createNativeQuery("ALTER SEQUENCE product_id_seq RESTART WITH 1").executeUpdate();
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

        // Create test product
        testProduct = new Product();
        testProduct.setDescription("Test Product");
        testProduct.setSku(UUID.randomUUID());
        testProduct.setUpdated(ZonedDateTime.now());
        testProduct.setCreated(ZonedDateTime.now());
        testProduct = productRepo.save(testProduct);

        // Authenticate and get token
        final AuthReqDTO authRequest = new AuthReqDTO("test@example.com", "password");

        MvcResult result = mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthRespDTO authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthRespDTO.class);

        authToken = "Bearer " + authResponse.accessToken();
    }

    @Nested
    @DisplayName("When accessing endpoints without authentication")
    class WhenAccessingEndpointsWithoutAuthentication {

        @Test
        @DisplayName("Then return 401 when getting all products")
        void thenReturn401WhenGettingAllProducts() throws Exception {
            mockMvc.perform(get("/products"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when getting product by ID")
        void thenReturn401WhenGettingProductById() throws Exception {
            mockMvc.perform(get("/products/" + testProduct.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Then return 401 when creating product")
        void thenReturn401WhenCreatingProduct() throws Exception {
            ProductDTO newProduct = new ProductDTO();
            newProduct.setDescription("New Product");
            newProduct.setSku(UUID.randomUUID());

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newProduct)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("When accessing endpoints with authentication")
    class WhenAccessingEndpointsWithAuthentication {

        @Test
        @DisplayName("Then return 200 when getting all products")
        void thenReturn200WhenGettingAllProducts() throws Exception {
            mockMvc.perform(get("/products")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(testProduct.getId()))
                    .andExpect(jsonPath("$[0].description").value(testProduct.getDescription()));
        }

        @Test
        @DisplayName("Then return 200 when getting product by ID")
        void thenReturn200WhenGettingProductById() throws Exception {
            mockMvc.perform(get("/products/" + testProduct.getId())
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testProduct.getId()))
                    .andExpect(jsonPath("$.description").value(testProduct.getDescription()));
        }

        @Test
        @DisplayName("Then return 200 when creating product")
        void thenReturn200WhenCreatingProduct() throws Exception {
            ProductDTO newProduct = new ProductDTO();
            newProduct.setDescription("New Product");
            newProduct.setSku(UUID.randomUUID());

            mockMvc.perform(post("/products")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newProduct)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("New Product"))
                    .andExpect(jsonPath("$.sku").isNotEmpty())
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }
    }
}