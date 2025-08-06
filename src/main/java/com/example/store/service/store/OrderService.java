package com.example.store.service.store;

import com.example.store.dto.OrderDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    List<OrderDTO> findAllOrders(Pageable pageable);

    OrderDTO findOrderById(Long id);

    OrderDTO createOrder(OrderDTO order);
    
    void clearOrdersCache();
}
