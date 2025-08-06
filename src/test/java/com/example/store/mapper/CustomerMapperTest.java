package com.example.store.mapper;

import com.example.store.dto.CustomerDTO;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Unit Test - CustomerMapper")
class CustomerMapperTest {

    private final CustomerMapper mapper = Mappers.getMapper(CustomerMapper.class);

    @Nested
    @DisplayName("When mapping Customer to CustomerDTO")
    class WhenMappingCustomerToCustomerDTO {

        @Test
        @DisplayName("Then correctly map all fields")
        void thenCorrectlyMapAllFields() {
            // Given
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setName("Test Customer");
            ZonedDateTime now = ZonedDateTime.now();
            customer.setCreated(now);
            customer.setUpdated(now);

            // Create some orders for the customer
            Set<Order> orders = new HashSet<>();
            Order order1 = new Order();
            order1.setId(101L);
            order1.setDescription("Order 1");
            
            Order order2 = new Order();
            order2.setId(102L);
            order2.setDescription("Order 2");
            
            orders.add(order1);
            orders.add(order2);
            customer.setOrders(orders);

            // When
            CustomerDTO dto = mapper.toCustomerDTO(customer);

            // Then
            assertNotNull(dto);
            assertEquals(customer.getId(), dto.getId());
            assertEquals(customer.getName(), dto.getName());
            assertEquals(customer.getCreated(), dto.getCreated());
            assertEquals(customer.getUpdated(), dto.getUpdated());
            
            // Check that orders were mapped to IDs
            assertNotNull(dto.getOrders());
            assertEquals(2, dto.getOrders().size());
            assertTrue(dto.getOrders().contains(101L));
            assertTrue(dto.getOrders().contains(102L));
        }

        @Test
        @DisplayName("Then handle null orders")
        void thenHandleNullOrders() {
            // Given
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setName("Test Customer");
            customer.setOrders(null);

            // When
            CustomerDTO dto = mapper.toCustomerDTO(customer);

            // Then
            assertNotNull(dto);
            assertEquals(customer.getId(), dto.getId());
            assertEquals(customer.getName(), dto.getName());
            assertNull(dto.getOrders());
        }
    }

    @Nested
    @DisplayName("When mapping CustomerDTO to Customer")
    class WhenMappingCustomerDTOToCustomer {

        @Test
        @DisplayName("Then correctly map basic fields")
        void thenCorrectlyMapBasicFields() {
            // Given
            CustomerDTO dto = new CustomerDTO();
            dto.setId(1L);
            dto.setName("Test Customer");
            ZonedDateTime now = ZonedDateTime.now();
            dto.setCreated(now);
            dto.setUpdated(now);
            
            // Add some order IDs
            Set<Long> orderIds = new HashSet<>();
            orderIds.add(101L);
            orderIds.add(102L);
            dto.setOrders(orderIds);

            // When
            Customer customer = mapper.toCustomer(dto);

            // Then
            assertNotNull(customer);
            assertEquals(dto.getId(), customer.getId());
            assertEquals(dto.getName(), customer.getName());
            assertEquals(dto.getCreated(), customer.getCreated());
            assertEquals(dto.getUpdated(), customer.getUpdated());
            
            // Orders should be ignored in this mapping
            assertNotNull(customer.getOrders());
            assertTrue(customer.getOrders().isEmpty());
        }
    }

    @Nested
    @DisplayName("When mapping List of Customers to DTOs")
    class WhenMappingCustomerList {

        @Test
        @DisplayName("Then correctly map all customers")
        void thenCorrectlyMapAllCustomers() {
            // Given
            Customer customer1 = new Customer();
            customer1.setId(1L);
            customer1.setName("Customer 1");
            
            Customer customer2 = new Customer();
            customer2.setId(2L);
            customer2.setName("Customer 2");
            
            List<Customer> customers = Arrays.asList(customer1, customer2);

            // When
            List<CustomerDTO> dtos = mapper.toCustomerDTOs(customers);

            // Then
            assertNotNull(dtos);
            assertEquals(2, dtos.size());
            assertEquals("Customer 1", dtos.get(0).getName());
            assertEquals("Customer 2", dtos.get(1).getName());
        }

        @Test
        @DisplayName("Then handle empty list")
        void thenHandleEmptyList() {
            // Given
            List<Customer> customers = List.of();

            // When
            List<CustomerDTO> dtos = mapper.toCustomerDTOs(customers);

            // Then
            assertNotNull(dtos);
            assertTrue(dtos.isEmpty());
        }
    }

    @Nested
    @DisplayName("When using custom mapping methods")
    class WhenUsingCustomMappingMethods {

        @Test
        @DisplayName("Then mapOrdersToIds correctly maps orders to IDs")
        void thenMapOrdersToIdsWorksCorrectly() {
            // Given
            Set<Order> orders = new HashSet<>();
            Order order1 = new Order();
            order1.setId(101L);
            
            Order order2 = new Order();
            order2.setId(102L);
            
            orders.add(order1);
            orders.add(order2);

            // When
            Set<Long> orderIds = mapper.mapOrdersToIds(orders);

            // Then
            assertNotNull(orderIds);
            assertEquals(2, orderIds.size());
            assertTrue(orderIds.contains(101L));
            assertTrue(orderIds.contains(102L));
        }

        @Test
        @DisplayName("Then mapOrdersToIds handles null input")
        void thenMapOrdersToIdsHandlesNull() {
            // When
            Set<Long> orderIds = mapper.mapOrdersToIds(null);

            // Then
            assertNull(orderIds);
        }
    }
}