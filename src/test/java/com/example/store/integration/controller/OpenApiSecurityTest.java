package com.example.store.integration.controller;

import com.example.store.StoreApplication;
import com.example.store.integration.config.IntTestConfig;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StoreApplication.class)
@AutoConfigureMockMvc
@Tag("int")
@DisplayName("Integration Test - OpenAPI Documentation Security")
@Transactional
@Testcontainers
@ActiveProfiles("int")
@Import(IntTestConfig.class)
class OpenApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("When accessing OpenAPI endpoints without authentication")
    class WhenAccessingOpenApiEndpointsWithoutAuthentication {

        @Test
        @DisplayName("Then return 200 when accessing OpenAPI JSON documentation")
        void thenReturn200WhenAccessingOpenApiJsonDocumentation() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Then return 200 when accessing OpenAPI UI HTML page")
        void thenReturn200WhenAccessingOpenApiUiHtmlPage() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        }

        @Test
        @DisplayName("Then return 200 when accessing Swagger UI resources")
        void thenReturn200WhenAccessingSwaggerUiResources() throws Exception {
            mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/css"));
        }

        @Test
        @DisplayName("Then return 200 when accessing OpenAPI UI configuration")
        void thenReturn200WhenAccessingOpenApiUiConfiguration() throws Exception {
            mockMvc.perform(get("/v3/api-docs/swagger-config"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}