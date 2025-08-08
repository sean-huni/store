package com.example.store.controller;

import com.example.store.component.GlobalSearchProps;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
import com.example.store.service.store.OrderService;
import com.example.store.util.PageableBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OrderControllerTests {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private CustomerRepo customerRepo;
    
    @Mock
    private OrderService orderService;
    
    @Mock
    private GlobalSearchProps globalSearchProps;
    
    @Mock
    private PageableBuilder pageableBuilder;
    
    @InjectMocks
    private OrderController orderController;

    private Order order;
    private Customer customer;

    @BeforeEach
    void setUp() {
        // Set up GlobalSearchProps mock with lenient to avoid unnecessary stubbing errors
        lenient().when(globalSearchProps.getLimit()).thenReturn(20);
        lenient().when(globalSearchProps.getSortField()).thenReturn("id");
        lenient().when(globalSearchProps.getDirection()).thenReturn("asc");
        
        // Set up PageableBuilder mock with lenient
        lenient().when(pageableBuilder.buildPageable(any(), any(), any(), any(), anyInt(), anyString(), anyString()))
                .thenReturn(PageRequest.of(0, 20, Sort.by("id").ascending()));
        
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        
        customer = new Customer();
        customer.setName("John Doe");
        customer.setId(1L);

        order = new Order();
        order.setDescription("Test Order");
        order.setId(1L);
        order.setCustomer(customer);
    }

    @Test
    void testCreateOrder() throws Exception {
        // Create OrderDTO with required fields
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomerId(1L);
        
        // Mock the service to return an OrderDTO with customer info
        OrderDTO returnedOrderDTO = new OrderDTO();
        returnedOrderDTO.setDescription("Test Order");
        returnedOrderDTO.setCustomerId(1L);
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName("John Doe");
        customerDTO.setId(1L);
        returnedOrderDTO.setCustomer(customerDTO);
        
        when(orderService.createOrder(any(OrderDTO.class))).thenReturn(returnedOrderDTO);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetOrder() throws Exception {
        // Create OrderDTO list to return from service
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomerId(1L);
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setName("John Doe");
        customerDTO.setId(1L);
        orderDTO.setCustomer(customerDTO);
        
        when(orderService.findAllOrders(any())).thenReturn(List.of(orderDTO));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test Order"))
                .andExpect(jsonPath("$[0].customer.name").value("John Doe"));
    }
}
