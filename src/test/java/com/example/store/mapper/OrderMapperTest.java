package com.example.store.mapper;

import com.example.store.dto.OrderDTO;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Unit Test - OrderMapper")
class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Nested
    @DisplayName("When mapping Order to OrderDTO")
    class WhenMappingOrderToOrderDTO {

        @Test
        @DisplayName("Then correctly map all fields")
        void thenCorrectlyMapAllFields() {
            // Given
            Order order = new Order();
            order.setId(1L);
            order.setDescription("Test Order");
            ZonedDateTime now = ZonedDateTime.now();
            order.setCreated(now);
            order.setUpdated(now);

            // Create a customer for the order
            Customer customer = new Customer();
            customer.setId(101L);
            customer.setName("Test Customer");
            order.setCustomer(customer);

            // Create product orders
            List<ProductOrder> productOrders = new ArrayList<>();
            
            Product product1 = new Product();
            product1.setId(201L);
            product1.setDescription("Product 1");
            
            Product product2 = new Product();
            product2.setId(202L);
            product2.setDescription("Product 2");
            
            ProductOrder po1 = new ProductOrder();
            po1.setId(301L);
            po1.setOrder(order);
            po1.setProduct(product1);
            po1.setQuantity(2);
            po1.setPrice(BigDecimal.valueOf(19.99));
            
            ProductOrder po2 = new ProductOrder();
            po2.setId(302L);
            po2.setOrder(order);
            po2.setProduct(product2);
            po2.setQuantity(1);
            po2.setPrice(BigDecimal.valueOf(29.99));
            
            productOrders.add(po1);
            productOrders.add(po2);
            order.setProducts(productOrders);

            // When
            OrderDTO dto = mapper.toOrderDTO(order);

            // Then
            assertNotNull(dto);
            assertEquals(order.getId(), dto.getId());
            assertEquals(order.getDescription(), dto.getDescription());
            assertEquals(order.getCreated(), dto.getCreated());
            assertEquals(order.getUpdated(), dto.getUpdated());
            assertEquals(customer.getId(), dto.getCustomerId());
            
            // Check that product orders were mapped to product IDs
            assertNotNull(dto.getProductIds());
            assertEquals(2, dto.getProductIds().size());
            assertTrue(dto.getProductIds().contains(201L));
            assertTrue(dto.getProductIds().contains(202L));
        }

        @Test
        @DisplayName("Then handle null products")
        void thenHandleNullProducts() {
            // Given
            Order order = new Order();
            order.setId(1L);
            order.setDescription("Test Order");
            
            Customer customer = new Customer();
            customer.setId(101L);
            order.setCustomer(customer);
            
            order.setProducts(null);

            // When
            OrderDTO dto = mapper.toOrderDTO(order);

            // Then
            assertNotNull(dto);
            assertEquals(order.getId(), dto.getId());
            assertEquals(order.getDescription(), dto.getDescription());
            assertEquals(customer.getId(), dto.getCustomerId());
            assertNull(dto.getProductIds());
        }
    }

    @Nested
    @DisplayName("When mapping OrderDTO to Order")
    class WhenMappingOrderDTOToOrder {

        @Test
        @DisplayName("Then correctly map basic fields")
        void thenCorrectlyMapBasicFields() {
            // Given
            OrderDTO dto = new OrderDTO();
            dto.setId(1L);
            dto.setDescription("Test Order");
            dto.setCustomerId(101L);
            ZonedDateTime now = ZonedDateTime.now();
            dto.setCreated(now);
            dto.setUpdated(now);
            
            // Add some product IDs
            Set<Long> productIds = new HashSet<>();
            productIds.add(201L);
            productIds.add(202L);
            dto.setProductIds(productIds);

            // When
            Order order = mapper.toOrder(dto);

            // Then
            assertNotNull(order);
            assertEquals(dto.getId(), order.getId());
            assertEquals(dto.getDescription(), order.getDescription());
            assertEquals(dto.getCreated(), order.getCreated());
            assertEquals(dto.getUpdated(), order.getUpdated());
            
            // Customer and products should be ignored in this mapping
            assertNull(order.getCustomer());
            assertNotNull(order.getProducts());
            assertTrue(order.getProducts().isEmpty());
        }
    }

    @Nested
    @DisplayName("When mapping List of Orders to DTOs")
    class WhenMappingOrderList {

        @Test
        @DisplayName("Then correctly map all orders")
        void thenCorrectlyMapAllOrders() {
            // Given
            Order order1 = new Order();
            order1.setId(1L);
            order1.setDescription("Order 1");
            Customer customer1 = new Customer();
            customer1.setId(101L);
            order1.setCustomer(customer1);
            
            Order order2 = new Order();
            order2.setId(2L);
            order2.setDescription("Order 2");
            Customer customer2 = new Customer();
            customer2.setId(102L);
            order2.setCustomer(customer2);
            
            List<Order> orders = Arrays.asList(order1, order2);

            // When
            List<OrderDTO> dtos = mapper.ordersToOrderDTOs(orders);

            // Then
            assertNotNull(dtos);
            assertEquals(2, dtos.size());
            assertEquals("Order 1", dtos.get(0).getDescription());
            assertEquals(101L, dtos.get(0).getCustomerId());
            assertEquals("Order 2", dtos.get(1).getDescription());
            assertEquals(102L, dtos.get(1).getCustomerId());
        }

        @Test
        @DisplayName("Then handle empty list")
        void thenHandleEmptyList() {
            // Given
            List<Order> orders = List.of();

            // When
            List<OrderDTO> dtos = mapper.ordersToOrderDTOs(orders);

            // Then
            assertNotNull(dtos);
            assertTrue(dtos.isEmpty());
        }
    }

    @Nested
    @DisplayName("When using custom mapping methods")
    class WhenUsingCustomMappingMethods {

        @Test
        @DisplayName("Then mapProductOrdersToProductIds correctly maps product orders to product IDs")
        void thenMapProductOrdersToProductIdsWorksCorrectly() {
            // Given
            List<ProductOrder> productOrders = new ArrayList<>();
            
            Order order = new Order();
            order.setId(1L);
            
            Product product1 = new Product();
            product1.setId(201L);
            
            Product product2 = new Product();
            product2.setId(202L);
            
            ProductOrder po1 = new ProductOrder();
            po1.setOrder(order);
            po1.setProduct(product1);
            
            ProductOrder po2 = new ProductOrder();
            po2.setOrder(order);
            po2.setProduct(product2);
            
            productOrders.add(po1);
            productOrders.add(po2);

            // When
            Set<Long> productIds = mapper.mapProductOrdersToProductIds(productOrders);

            // Then
            assertNotNull(productIds);
            assertEquals(2, productIds.size());
            assertTrue(productIds.contains(201L));
            assertTrue(productIds.contains(202L));
        }

        @Test
        @DisplayName("Then mapProductOrdersToProductIds handles null input")
        void thenMapProductOrdersToProductIdsHandlesNull() {
            // When
            Set<Long> productIds = mapper.mapProductOrdersToProductIds(null);

            // Then
            assertNull(productIds);
        }
    }
}