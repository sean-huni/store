package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quickperf.sql.annotation.ExpectSelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import test.config.TestConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("repo")
@ActiveProfiles("db")
@DataJpaTest
@Import(TestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("CustomerRepo - {Int}")
class CustomerRepoTest {
    @Autowired
    private CustomerRepo customerRepo;

    private static final int PAGE_SIZE = 10;

    @Nested
    @DisplayName("When searching customers by name")
    class WhenSearchCustomersByName {

        @Test
        @DisplayName("Then return first 10 customers by pagination")
        void thenReturnFirst10Customers() {
            // Create a pageable request for the first page with 10 items
            final Pageable pageable = PageRequest.of(0, PAGE_SIZE);

            // Search for customers with a common substring that should exist in the database
            // The database is pre-populated with customer data from data.sql
            final String searchTerm = "a";
            final List<Customer> customers = customerRepo.findCustomersByNameContainingIgnoreCase(searchTerm, pageable);

            // Verify the results
            assertNotNull(customers);
            assertFalse(customers.isEmpty());
            assertTrue(customers.size() <= PAGE_SIZE,
                    "Expected at most " + PAGE_SIZE + " customers, but got " + customers.size());

            // Verify all returned customers have our search term in their name
            for (Customer customer : customers) {
                assertTrue(customer.getName().toLowerCase().contains(searchTerm.toLowerCase()), "Customer name '%s' should contain '%s'".formatted(customer.getName(), searchTerm));
            }
        }

        @Test
        @DisplayName("Then return customers matching case-insensitive search")
        void thenReturnCustomersMatchingCaseInsensitiveSearch() {
            final Pageable pageable = PageRequest.of(0, PAGE_SIZE);

            final String searchTerm = "A"; // Uppercase version of previous search
            final List<Customer> customers = customerRepo.findCustomersByNameContainingIgnoreCase(searchTerm, pageable);

            assertNotNull(customers);
            assertFalse(customers.isEmpty());

            // Verify all returned customers have our search term in their name (case-insensitive)
            for (final Customer customer : customers) {
                assertTrue(customer.getName().toLowerCase().contains(searchTerm.toLowerCase()), "Customer name '%s' should contain '%s' (case-insensitive)".formatted(customer.getName(), searchTerm));
            }
        }
    }

    @Nested
    @DisplayName("When finding customer by ID (The N+1 Problem)")
    class WhenFindingCustomerById {

        @Test
        @DisplayName("Then return customer with orders when customer exists")
        @ExpectSelect(1)
        void thenReturnCustomerWithOrdersWhenCustomerExists() {
            // Given: Customer ID 13 has multiple orders (2, 7, 43, 61) based on test data
            final Long customerId = 13L;

            // When: Finding customer by ID
            final Optional<Customer> customerOptional = customerRepo.findCustomerById(customerId);

            // Then: Verify customer is found
            assertNotNull(customerOptional);
            assertTrue(customerOptional.isPresent(), "Customer should be found");

            final Customer customer = customerOptional.get();
            assertNotNull(customer, "Customer should not be null");
            assertEquals(customerId, customer.getId(), "Customer ID should match");
            assertEquals("Miguel VonRueden", customer.getName(), "Customer name should match test data");

            // Verify orders are loaded (EntityGraph should eagerly fetch orders)
            assertNotNull(customer.getOrders(), "Orders should not be null");
            assertFalse(customer.getOrders().isEmpty(), "Customer should have orders");

            // Customer 13 has 102 orders in test data based on generated data
            assertEquals(102, customer.getOrders().size(),
                    "Customer 13 should have exactly 102 orders based on test data");
        }

        @Test
        @DisplayName("Then return empty optional when customer does not exist")
        @ExpectSelect(1)
        void thenReturnEmptyOptionalWhenCustomerDoesNotExist() {
            // Given: A customer ID that doesn't exist (test data has customers 1-100)
            final Long nonExistentCustomerId = 999L;

            // When: Finding customer by ID
            final Optional<Customer> customerOptional = customerRepo.findCustomerById(nonExistentCustomerId);

            // Then: Verify customer is not found
            assertNotNull(customerOptional);
            assertFalse(customerOptional.isPresent(), "Customer should not be found for non-existent ID");
        }
    }
}