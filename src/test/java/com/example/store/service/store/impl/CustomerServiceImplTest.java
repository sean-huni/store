package com.example.store.service.store.impl;

import com.example.store.component.CustomerSearchProps;
import com.example.store.dto.CustomerDTO;
import com.example.store.mapper.CustomerMapper;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.repo.CustomerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - CustomerServiceImpl")
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerSearchProps customerSearchProps;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer testCustomer;
    private CustomerDTO testCustomerDTO;
    private List<Customer> customerList;
    private List<CustomerDTO> customerDTOList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create test customer
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setName("Test Customer");
        testCustomer.setCreated(ZonedDateTime.now());
        testCustomer.setUpdated(ZonedDateTime.now());
        testCustomer.setOrders(new HashSet<>());

        // Create test customer DTO
        testCustomerDTO = new CustomerDTO();
        testCustomerDTO.setId(1L);
        testCustomerDTO.setName("Test Customer");
        testCustomerDTO.setCreated(testCustomer.getCreated());
        testCustomerDTO.setUpdated(testCustomer.getUpdated());
        testCustomerDTO.setOrders(new HashSet<>());

        // Create list of customers
        customerList = new ArrayList<>();
        customerList.add(testCustomer);

        // Create list of customer DTOs
        customerDTOList = new ArrayList<>();
        customerDTOList.add(testCustomerDTO);

        // Create pageable mock
        pageable = Pageable.ofSize(10);
    }

    @Nested
    @DisplayName("When finding all customers")
    class WhenFindingAllCustomers {

        @Test
        @DisplayName("Then return list of customer DTOs")
        void thenReturnListOfCustomerDTOs() {
            // Given
            Page<Customer> customerPage = new PageImpl<>(customerList);
            when(customerRepo.findAll(any(Pageable.class))).thenReturn(customerPage);
            when(customerMapper.toCustomerDTOs(customerList)).thenReturn(customerDTOList);

            // When
            List<CustomerDTO> result = customerService.findAllCustomers(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCustomerDTO, result.get(0));
            verify(customerRepo, times(1)).findAll(pageable);
            verify(customerMapper, times(1)).toCustomerDTOs(customerList);
        }
    }

    @Nested
    @DisplayName("When finding customers by name")
    class WhenFindingCustomersByName {

        @Test
        @DisplayName("Then return matching customers")
        void thenReturnMatchingCustomers() {
            // Given
            String searchName = "Test";
            when(customerRepo.findCustomersByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                    .thenReturn(customerList);
            when(customerMapper.toCustomerDTOs(customerList)).thenReturn(customerDTOList);

            // When
            List<CustomerDTO> result = customerService.findCustomersNameContainingSubString(searchName, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testCustomerDTO, result.get(0));
            verify(customerRepo, times(1))
                    .findCustomersByNameContainingIgnoreCase(searchName, pageable);
            verify(customerMapper, times(1)).toCustomerDTOs(customerList);
        }
    }

    @Nested
    @DisplayName("When creating a customer")
    class WhenCreatingCustomer {

        @Test
        @DisplayName("Then save and return the customer")
        void thenSaveAndReturnCustomer() {
            // Given
            when(customerMapper.toCustomer(any(CustomerDTO.class))).thenReturn(testCustomer);
            when(customerRepo.save(any(Customer.class))).thenReturn(testCustomer);
            when(customerMapper.toCustomerDTO(any(Customer.class))).thenReturn(testCustomerDTO);

            // When
            CustomerDTO result = customerService.createCustomer(testCustomerDTO);

            // Then
            assertNotNull(result);
            assertEquals(testCustomerDTO, result);
            verify(customerMapper, times(1)).toCustomer(testCustomerDTO);
            verify(customerRepo, times(1)).save(testCustomer);
            verify(customerMapper, times(1)).toCustomerDTO(testCustomer);
        }
    }

    @Nested
    @DisplayName("When finding customer by ID")
    class WhenFindingCustomerById {

        @Test
        @DisplayName("Then return customer when found")
        void thenReturnCustomerWhenFound() {
            // Given
            when(customerRepo.findById(anyLong())).thenReturn(Optional.of(testCustomer));
            when(customerMapper.toCustomerDTO(any(Customer.class))).thenReturn(testCustomerDTO);

            // When
            CustomerDTO result = customerService.findCustomerById(1L);

            // Then
            assertNotNull(result);
            assertEquals(testCustomerDTO, result);
            verify(customerRepo, times(1)).findById(1L);
            verify(customerMapper, times(1)).toCustomerDTO(testCustomer);
        }

        @Test
        @DisplayName("Then return null when not found")
        void thenReturnNullWhenNotFound() {
            // Given
            when(customerRepo.findById(anyLong())).thenReturn(Optional.empty());

            // When
            CustomerDTO result = customerService.findCustomerById(999L);

            // Then
            assertNull(result);
            verify(customerRepo, times(1)).findById(999L);
        }
    }
}