package com.example.store.controller;

import com.example.store.component.CustomerSearchProps;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.SortEnumDTO;
import com.example.store.service.store.CustomerService;
import com.example.store.util.PageableBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("CustomerController - {Unit}")
@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock(lenient = true)
    private CustomerService customerService;

    @Mock(lenient = true)
    private CustomerSearchProps customerSearchProps;

    @Mock(lenient = true)
    private PageableBuilder pageableBuilder;

    @InjectMocks
    private CustomerController customerController;

    private CustomerDTO testCustomer;
    private List<CustomerDTO> customerList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = new CustomerDTO();
        testCustomer.setId(1L);
        testCustomer.setName("Test Customer");
        testCustomer.setOrders(new HashSet<>());

        // Create list of customers
        customerList = new ArrayList<>();
        customerList.add(testCustomer);

        // Create pageable
        pageable = Pageable.ofSize(10);

        // Set up default properties
        when(customerSearchProps.getLimit()).thenReturn(20);
        when(customerSearchProps.getSortField()).thenReturn("name");
        when(customerSearchProps.getDirection()).thenReturn("asc");

        // Set up pageable builder
        when(pageableBuilder.buildPageable(
                any(Integer.class), 
                any(Integer.class), 
                anyString(), 
                any(SortEnumDTO.class), 
                eq(20), 
                eq("name"), 
                eq("asc")))
                .thenReturn(pageable);
    }

    @Nested
    @DisplayName("When finding customers")
    class WhenFindingCustomers {

        @Test
        @DisplayName("Then return all customers when name is null")
        void thenReturnAllCustomersWhenNameIsNull() {
            // Given
            when(customerService.findAllCustomers(any(Pageable.class))).thenReturn(customerList);

            // When
            List<CustomerDTO> result = customerController.findCustomers(null, 0, 10, "name", SortEnumDTO.asc);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCustomer, result.get(0));
            verify(pageableBuilder, times(1)).buildPageable(eq(0), eq(10), eq("name"), eq(SortEnumDTO.asc), 
                    eq(20), eq("name"), eq("asc"));
            verify(customerService, times(1)).findAllCustomers(pageable);
            verify(customerService, times(0)).findCustomersNameContainingSubString(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("Then return filtered customers when name is provided")
        void thenReturnFilteredCustomersWhenNameIsProvided() {
            // Given
            String searchName = "Test";
            when(customerService.findCustomersNameContainingSubString(anyString(), any(Pageable.class))).thenReturn(customerList);

            // When
            List<CustomerDTO> result = customerController.findCustomers(searchName, 0, 10, "name", SortEnumDTO.asc);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCustomer, result.get(0));
            verify(pageableBuilder, times(1)).buildPageable(eq(0), eq(10), eq("name"), eq(SortEnumDTO.asc), 
                    eq(20), eq("name"), eq("asc"));
            verify(customerService, times(0)).findAllCustomers(any(Pageable.class));
            verify(customerService, times(1)).findCustomersNameContainingSubString(eq(searchName), eq(pageable));
        }
    }

    @Nested
    @DisplayName("When finding customer by ID")
    class WhenFindingCustomerById {

        @Test
        @DisplayName("Then return customer when found")
        void thenReturnCustomerWhenFound() {
            // Given
            Long customerId = 1L;
            when(customerService.findCustomerById(customerId)).thenReturn(testCustomer);

            // When
            CustomerDTO result = customerController.findCustomerById(customerId);

            // Then
            assertNotNull(result);
            assertEquals(testCustomer, result);
            verify(customerService, times(1)).findCustomerById(customerId);
        }
    }

    @Nested
    @DisplayName("When creating a customer")
    class WhenCreatingCustomer {

        @Test
        @DisplayName("Then save and return the customer")
        void thenSaveAndReturnCustomer() {
            // Given
            CustomerDTO newCustomer = new CustomerDTO();
            newCustomer.setName("New Customer");
            
            when(customerService.createCustomer(any(CustomerDTO.class))).thenReturn(newCustomer);

            // When
            CustomerDTO result = customerController.createCustomer(newCustomer);

            // Then
            assertNotNull(result);
            assertSame(newCustomer, result);
            verify(customerService, times(1)).createCustomer(newCustomer);
        }
    }
}