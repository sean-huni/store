package com.example.store.controller;

import com.example.store.component.CustomerSearchProps;
import com.example.store.dto.CustomerDTO;
import com.example.store.mapper.CustomerMapper;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.service.store.CustomerService;
import com.example.store.util.PageableBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CustomerControllerTests {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CustomerRepo customerRepo;
    
    @Mock
    private CustomerMapper customerMapper;
    
    @Mock
    private CustomerService customerService;
    
    @Mock
    private CustomerSearchProps customerSearchProps;
    
    @Mock
    private PageableBuilder pageableBuilder;
    
    @InjectMocks
    private CustomerController customerController;

    private Customer customer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();
        
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);
    }

    @Test
    void testCreateCustomer() throws Exception {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName("John Doe");
        
        when(customerService.createCustomer(any(CustomerDTO.class))).thenReturn(customerDTO);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void testGetAllCustomers() throws Exception {
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName("John Doe");
        
        // Set up pageableBuilder mock
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.ofSize(10);
        when(customerSearchProps.getLimit()).thenReturn(20);
        when(customerSearchProps.getSortField()).thenReturn("name");
        when(customerSearchProps.getDirection()).thenReturn("asc");
        
        // Use specific values instead of matchers
        when(pageableBuilder.buildPageable(
                null, 
                null, 
                null, 
                null, 
                20, 
                "name", 
                "asc")).thenReturn(pageable);
        
        // Set up customerService mock
        when(customerService.findAllCustomers(any())).thenReturn(List.of(customerDTO));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..name").value("John Doe"));
    }
}
