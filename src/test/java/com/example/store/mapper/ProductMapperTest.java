package com.example.store.mapper;

import com.example.store.dto.ProductDTO;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Unit Test - ProductMapper")
class ProductMapperTest {

    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Nested
    @DisplayName("When mapping Product to ProductDTO")
    class WhenMappingProductToProductDTO {

        @Test
        @DisplayName("Then correctly map all fields")
        void thenCorrectlyMapAllFields() {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setDescription("Test Product");
            UUID sku = UUID.randomUUID();
            product.setSku(sku);
            ZonedDateTime now = ZonedDateTime.now();
            product.setCreated(now);
            product.setUpdated(now);

            // Create product orders
            List<ProductOrder> productOrders = new ArrayList<>();
            
            Order order1 = new Order();
            order1.setId(101L);
            order1.setDescription("Order 1");
            
            Order order2 = new Order();
            order2.setId(102L);
            order2.setDescription("Order 2");
            
            ProductOrder po1 = new ProductOrder();
            po1.setId(201L);
            po1.setProduct(product);
            po1.setOrder(order1);
            
            ProductOrder po2 = new ProductOrder();
            po2.setId(202L);
            po2.setProduct(product);
            po2.setOrder(order2);
            
            productOrders.add(po1);
            productOrders.add(po2);
            product.setOrders(productOrders);

            // When
            ProductDTO dto = mapper.toProductDTO(product);

            // Then
            assertNotNull(dto);
            assertEquals(product.getId(), dto.getId());
            assertEquals(product.getDescription(), dto.getDescription());
            assertEquals(product.getSku(), dto.getSku());
            assertEquals(product.getCreated(), dto.getCreated());
            assertEquals(product.getUpdated(), dto.getUpdated());
            
            // Check that product orders were mapped to order IDs
            assertNotNull(dto.getOrderIds());
            assertEquals(2, dto.getOrderIds().size());
            assertTrue(dto.getOrderIds().contains(101L));
            assertTrue(dto.getOrderIds().contains(102L));
        }

        @Test
        @DisplayName("Then handle null orders")
        void thenHandleNullOrders() {
            // Given
            Product product = new Product();
            product.setId(1L);
            product.setDescription("Test Product");
            product.setSku(UUID.randomUUID());
            product.setOrders(null);

            // When
            ProductDTO dto = mapper.toProductDTO(product);

            // Then
            assertNotNull(dto);
            assertEquals(product.getId(), dto.getId());
            assertEquals(product.getDescription(), dto.getDescription());
            assertEquals(product.getSku(), dto.getSku());
            assertNotNull(dto.getOrderIds());
            assertTrue(dto.getOrderIds().isEmpty());
        }
    }

    @Nested
    @DisplayName("When mapping ProductDTO to Product")
    class WhenMappingProductDTOToProduct {

        @Test
        @DisplayName("Then correctly map basic fields")
        void thenCorrectlyMapBasicFields() {
            // Given
            ProductDTO dto = new ProductDTO();
            dto.setId(1L);
            dto.setDescription("Test Product");
            UUID sku = UUID.randomUUID();
            dto.setSku(sku);
            ZonedDateTime now = ZonedDateTime.now();
            dto.setCreated(now);
            dto.setUpdated(now);
            
            // Add some order IDs
            Set<Long> orderIds = new HashSet<>();
            orderIds.add(101L);
            orderIds.add(102L);
            dto.setOrderIds(orderIds);

            // When
            Product product = mapper.toProduct(dto);

            // Then
            assertNotNull(product);
            assertEquals(dto.getId(), product.getId());
            assertEquals(dto.getDescription(), product.getDescription());
            assertEquals(dto.getSku(), product.getSku());
            assertEquals(dto.getCreated(), product.getCreated());
            assertEquals(dto.getUpdated(), product.getUpdated());
            
            // Orders should be ignored in this mapping
            assertNotNull(product.getOrders());
            assertTrue(product.getOrders().isEmpty());
        }
    }

    @Nested
    @DisplayName("When mapping List of Products to DTOs")
    class WhenMappingProductList {

        @Test
        @DisplayName("Then correctly map all products")
        void thenCorrectlyMapAllProducts() {
            // Given
            Product product1 = new Product();
            product1.setId(1L);
            product1.setDescription("Product 1");
            product1.setSku(UUID.randomUUID());
            
            Product product2 = new Product();
            product2.setId(2L);
            product2.setDescription("Product 2");
            product2.setSku(UUID.randomUUID());
            
            List<Product> products = Arrays.asList(product1, product2);

            // When
            List<ProductDTO> dtos = mapper.toProductDTOList(products);

            // Then
            assertNotNull(dtos);
            assertEquals(2, dtos.size());
            assertEquals("Product 1", dtos.get(0).getDescription());
            assertEquals("Product 2", dtos.get(1).getDescription());
            assertEquals(product1.getSku(), dtos.get(0).getSku());
            assertEquals(product2.getSku(), dtos.get(1).getSku());
        }

        @Test
        @DisplayName("Then handle empty list")
        void thenHandleEmptyList() {
            // Given
            List<Product> products = List.of();

            // When
            List<ProductDTO> dtos = mapper.toProductDTOList(products);

            // Then
            assertNotNull(dtos);
            assertTrue(dtos.isEmpty());
        }
    }

    @Nested
    @DisplayName("When using custom mapping methods")
    class WhenUsingCustomMappingMethods {

        @Test
        @DisplayName("Then mapProductOrdersToOrderIds correctly maps product orders to order IDs")
        void thenMapProductOrdersToOrderIdsWorksCorrectly() {
            // Given
            List<ProductOrder> productOrders = new ArrayList<>();
            
            Product product = new Product();
            product.setId(1L);
            
            Order order1 = new Order();
            order1.setId(101L);
            
            Order order2 = new Order();
            order2.setId(102L);
            
            ProductOrder po1 = new ProductOrder();
            po1.setProduct(product);
            po1.setOrder(order1);
            
            ProductOrder po2 = new ProductOrder();
            po2.setProduct(product);
            po2.setOrder(order2);
            
            productOrders.add(po1);
            productOrders.add(po2);

            // When
            Set<Long> orderIds = mapper.mapProductOrdersToOrderIds(productOrders);

            // Then
            assertNotNull(orderIds);
            assertEquals(2, orderIds.size());
            assertTrue(orderIds.contains(101L));
            assertTrue(orderIds.contains(102L));
        }

        @Test
        @DisplayName("Then mapProductOrdersToOrderIds handles null input")
        void thenMapProductOrdersToOrderIdsHandlesNull() {
            // When
            Set<Long> orderIds = mapper.mapProductOrdersToOrderIds(null);

            // Then
            assertNotNull(orderIds);
            assertTrue(orderIds.isEmpty());
        }
        
        @Test
        @DisplayName("Then generateSkuIfMissing returns input SKU when not null")
        void thenGenerateSkuIfMissingReturnsInputSku() {
            // Given
            UUID sku = UUID.randomUUID();
            
            // When
            UUID result = mapper.generateSkuIfMissing(sku);
            
            // Then
            assertEquals(sku, result);
        }
        
        @Test
        @DisplayName("Then generateSkuIfMissing generates new SKU when input is null")
        void thenGenerateSkuIfMissingGeneratesNewSku() {
            // When
            UUID result = mapper.generateSkuIfMissing(null);
            
            // Then
            assertNotNull(result);
        }
    }
}