package com.example.store.controller;

import com.example.store.component.GlobalSearchProps;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.SortEnumDTO;
import com.example.store.service.store.OrderService;
import com.example.store.util.PageableBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;
    private final GlobalSearchProps globalSearchProps;
    private final PageableBuilder pageableBuilder;

    @GetMapping
    public List<OrderDTO> findOrders(
            @RequestParam(required = false) @Min(value = 0, message = "global.400.006") Integer page,
            @RequestParam(required = false) @Min(value = 5, message = "global.400.005") Integer limit,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) SortEnumDTO sortDir) {

        final Pageable pageable = pageableBuilder.buildPageable(page, limit, sortBy, sortDir, globalSearchProps.getLimit(),
                globalSearchProps.getSortField(),
                globalSearchProps.getDirection()
        );

        return orderService.findAllOrders(pageable);
    }

    @GetMapping("{id}")
    public OrderDTO getOrderById(@PathVariable("id") @Positive(message = "global.400.003") Long id) {
        return orderService.findOrderById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        return orderService.createOrder(orderDTO);
    }
}