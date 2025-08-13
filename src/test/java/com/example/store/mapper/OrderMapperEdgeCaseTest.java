package com.example.store.mapper;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@DisplayName("Unit Test - OrderMapper Edge Cases")
class OrderMapperEdgeCaseTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    @DisplayName("Should handle order with null customer when mapping to DTO")
    void shouldHandleOrderWithNullCustomerWhenMappingToDTO() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setDescription("Test Order");
        order.setCustomer(null);
        order.setProducts(new ArrayList<>());

        // When
        OrderDTO dto = mapper.toOrderDTO(order);

        // Then
        assertNotNull(dto);
        assertEquals(order.getId(), dto.getId());
        assertEquals(order.getDescription(), dto.getDescription());
        assertNull(dto.getCustomerId());
        assertNotNull(dto.getProductIds());
        assertTrue(dto.getProductIds().isEmpty());
    }

    @Test
    @DisplayName("Should handle product orders with null products when mapping to DTO")
    void shouldHandleProductOrdersWithNullProductsWhenMappingToDTO() {
        // Given
        Order order = new Order();
        order.setId(1L);
        order.setDescription("Test Order");

        Customer customer = new Customer();
        customer.setId(101L);
        order.setCustomer(customer);

        List<ProductOrder> productOrders = new ArrayList<>();
        ProductOrder po = new ProductOrder();
        po.setId(301L);
        po.setOrder(order);
        po.setProduct(null); // Null product
        productOrders.add(po);
        order.setProducts(productOrders);

        // When
        OrderDTO dto = mapper.toOrderDTO(order);

        // Then
        assertNotNull(dto);
        assertEquals(order.getId(), dto.getId());
        assertEquals(order.getDescription(), dto.getDescription());
        assertEquals(customer.getId(), dto.getCustomerId());
        assertNotNull(dto.getProductIds());
        assertTrue(dto.getProductIds().isEmpty());
    }

    @Test
    @DisplayName("Should handle order DTO with null customer ID when mapping to entity")
    void shouldHandleOrderDTOWithNullCustomerIdWhenMappingToEntity() {
        // Given
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);
        dto.setDescription("Test Order");
        dto.setCustomerId(null);
        dto.setProductIds(new HashSet<>());

        // When
        Order order = mapper.toOrder(dto);

        // Then
        assertNotNull(order);
        assertEquals(dto.getId(), order.getId());
        assertEquals(dto.getDescription(), order.getDescription());
        assertNull(order.getCustomer());
        assertNotNull(order.getProducts());
        assertTrue(order.getProducts().isEmpty());
    }

    @Test
    @DisplayName("Should handle order DTO with customer object when mapping to entity")
    void shouldHandleOrderDTOWithCustomerObjectWhenMappingToEntity() {
        // Given
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);
        dto.setDescription("Test Order");
        dto.setCustomerId(101L);

        // Set a customer object (this should be ignored in the mapping)
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(101L);
        customerDTO.setName("Test Customer");

        dto.setProductIds(new HashSet<>());

        // When
        Order order = mapper.toOrder(dto);

        // Then
        assertNotNull(order);
        assertEquals(dto.getId(), order.getId());
        assertEquals(dto.getDescription(), order.getDescription());
        assertNull(order.getCustomer()); // Customer should be ignored
        assertNotNull(order.getProducts());
        assertTrue(order.getProducts().isEmpty());
    }

    @Test
    @DisplayName("Should handle product orders with null product IDs when mapping to entity")
    void shouldHandleProductOrdersWithNullProductIdsWhenMappingToEntity() {
        // Given
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);
        dto.setDescription("Test Order");
        dto.setCustomerId(101L);
        dto.setProductIds(null);

        // When
        Order order = mapper.toOrder(dto);

        // Then
        assertNotNull(order);
        assertEquals(dto.getId(), order.getId());
        assertEquals(dto.getDescription(), order.getDescription());
        assertNull(order.getCustomer());
        assertNotNull(order.getProducts());
        assertTrue(order.getProducts().isEmpty());
    }

    @Test
    @DisplayName("Should handle product orders with null product when extracting IDs")
    void shouldHandleProductOrdersWithNullProductWhenExtractingIds() {
        // Given
        List<ProductOrder> productOrders = new ArrayList<>();

        // Add a product order with a valid product
        ProductOrder po1 = new ProductOrder();
        Product product1 = new Product();
        product1.setId(201L);
        po1.setProduct(product1);
        productOrders.add(po1);

        // Add a product order with a null product
        ProductOrder po2 = new ProductOrder();
        po2.setProduct(null);
        productOrders.add(po2);

        // When
        Set<Long> productIds = mapper.mapProductOrdersToProductIds(productOrders);

        // Then
        assertNotNull(productIds);
        assertEquals(1, productIds.size());
        assertTrue(productIds.contains(201L));
    }

    @Test
    @DisplayName("Should handle product orders with null product ID when extracting IDs")
    void shouldHandleProductOrdersWithNullProductIdWhenExtractingIds() {
        // Given
        List<ProductOrder> productOrders = new ArrayList<>();

        // Add a product order with a valid product ID
        ProductOrder po1 = new ProductOrder();
        Product product1 = new Product();
        product1.setId(201L);
        po1.setProduct(product1);
        productOrders.add(po1);

        // Add a product order with a product that has a null ID
        ProductOrder po2 = new ProductOrder();
        Product product2 = new Product();
        product2.setId(null);
        po2.setProduct(product2);
        productOrders.add(po2);

        // When
        Set<Long> productIds = mapper.mapProductOrdersToProductIds(productOrders);

        // Then
        assertNotNull(productIds);
        assertEquals(1, productIds.size());
        assertTrue(productIds.contains(201L));
    }
}