package com.example.store.service.store.impl;

import com.example.store.dto.OrderDTO;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.persistence.repo.OrderRepo;
import com.example.store.service.store.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepo orderRepo;
    private final OrderMapper orderMapper;
    private final CustomerRepo customerRepo;

    @Cacheable(value = "orders", key = "'all_page_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public List<OrderDTO> findAllOrders(final Pageable pageable) {
        final Page<Order> orderPage = orderRepo.findAll(pageable);
        return orderMapper.ordersToOrderDTOs(orderPage.getContent());
    }

    @Cacheable(value = "orders", key = "'id_' + #id")
    public OrderDTO findOrderById(final Long id) {
        return orderMapper.toOrderDTO(orderRepo.findById(id).orElse(null));
    }

    @CacheEvict(value = "orders", allEntries = true)
    public void clearOrdersCache() {
    }

    @CacheEvict(value = "orders", allEntries = true)
    public OrderDTO createOrder(final OrderDTO orderDTO) {
        // Check if customer exists
        final Customer customer = customerRepo.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("order.400.000", new Long[]{orderDTO.getCustomerId()}));

        final var order = orderMapper.toOrder(orderDTO);
        order.setCustomer(customer);

        // Set created and updated times
        final ZonedDateTime now = ZonedDateTime.now();
        order.setCreated(now);
        order.setUpdated(now);

        return orderMapper.toOrderDTO(orderRepo.save(order));
    }
}
