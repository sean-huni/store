package com.example.store.service.store.impl;

import com.example.store.dto.OrderDTO;
import com.example.store.mapper.OrderMapper;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - OrderServiceImpl")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepo orderRepo;

    @Mock
    private OrderMapper orderMapper;
    
    @Mock
    private CustomerRepo customerRepo;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDTO testOrderDTO;
    private List<Order> orderList;
    private List<OrderDTO> orderDTOList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create test customer
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Customer");

        // Create test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setDescription("Test Order");
        testOrder.setCustomer(customer);
        testOrder.setCreated(ZonedDateTime.now());
        testOrder.setUpdated(ZonedDateTime.now());
        testOrder.setProducts(new ArrayList<>());

        // Create test order DTO
        testOrderDTO = new OrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setDescription("Test Order");
        testOrderDTO.setCustomerId(1L);
        testOrderDTO.setCreated(testOrder.getCreated());
        testOrderDTO.setUpdated(testOrder.getUpdated());
        testOrderDTO.setProductIds(Set.of());

        // Create list of orders
        orderList = new ArrayList<>();
        orderList.add(testOrder);

        // Create list of order DTOs
        orderDTOList = new ArrayList<>();
        orderDTOList.add(testOrderDTO);

        // Create pageable mock
        pageable = Pageable.ofSize(10);
    }

    @Nested
    @DisplayName("When finding all orders")
    class WhenFindingAllOrders {

        @Test
        @DisplayName("Then return list of order DTOs")
        void thenReturnListOfOrderDTOs() {
            // Given
            Page<Order> orderPage = new PageImpl<>(orderList);
            when(orderRepo.findAll(any(Pageable.class))).thenReturn(orderPage);
            when(orderMapper.ordersToOrderDTOs(orderList)).thenReturn(orderDTOList);

            // When
            List<OrderDTO> result = orderService.findAllOrders(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testOrderDTO, result.get(0));
            verify(orderRepo, times(1)).findAll(pageable);
            verify(orderMapper, times(1)).ordersToOrderDTOs(orderList);
        }
    }

    @Nested
    @DisplayName("When finding order by ID")
    class WhenFindingOrderById {

        @Test
        @DisplayName("Then return order when found")
        void thenReturnOrderWhenFound() {
            // Given
            when(orderRepo.findById(anyLong())).thenReturn(Optional.of(testOrder));
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(testOrderDTO);

            // When
            OrderDTO result = orderService.findOrderById(1L);

            // Then
            assertNotNull(result);
            assertEquals(testOrderDTO, result);
            verify(orderRepo, times(1)).findById(1L);
            verify(orderMapper, times(1)).toOrderDTO(testOrder);
        }

        @Test
        @DisplayName("Then return null when not found")
        void thenReturnNullWhenNotFound() {
            // Given
            when(orderRepo.findById(anyLong())).thenReturn(Optional.empty());

            // When
            OrderDTO result = orderService.findOrderById(999L);

            // Then
            assertNull(result);
            verify(orderRepo, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("When creating an order")
    class WhenCreatingOrder {

        @Test
        @DisplayName("Then save and return the order")
        void thenSaveAndReturnOrder() {
            // Given
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setName("Test Customer");
            
            when(customerRepo.findById(anyLong())).thenReturn(Optional.of(customer));
            when(orderMapper.toOrder(any(OrderDTO.class))).thenReturn(testOrder);
            when(orderRepo.save(any(Order.class))).thenReturn(testOrder);
            when(orderMapper.toOrderDTO(any(Order.class))).thenReturn(testOrderDTO);

            // When
            OrderDTO result = orderService.createOrder(testOrderDTO);

            // Then
            assertNotNull(result);
            assertEquals(testOrderDTO, result);
            verify(customerRepo, times(1)).findById(testOrderDTO.getCustomerId());
            verify(orderMapper, times(1)).toOrder(testOrderDTO);
            verify(orderRepo, times(1)).save(testOrder);
            verify(orderMapper, times(1)).toOrderDTO(testOrder);
        }
    }
}