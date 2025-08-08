package com.example.store.controller;

import com.example.store.component.CustomerSearchProps;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.SortEnumDTO;
import com.example.store.service.store.CustomerService;
import com.example.store.util.PageableBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.Objects.isNull;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {
    private final CustomerService customerService;
    private final CustomerSearchProps customerSearchProps;
    private final PageableBuilder pageableBuilder;

    @GetMapping

    public List<CustomerDTO> findCustomers(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) @Min(value = 0, message = "global.400.006") final Integer page,
            @RequestParam(required = false) @Min(value = 5, message = "global.400.005") final Integer limit,
            @RequestParam(required = false) final String sortBy,
            @RequestParam(required = false) final SortEnumDTO sortDir) {

        final Pageable pageable = pageableBuilder.buildPageable(page, limit, sortBy, sortDir, customerSearchProps.getLimit(),
                customerSearchProps.getSortField(),
                customerSearchProps.getDirection()
        );

        if (isNull(name)) {
            return customerService.findAllCustomers(pageable);
        } else {
            return customerService.findCustomersNameContainingSubString(name, pageable);
        }
    }

    @GetMapping("{id}")
    public CustomerDTO findCustomerById(@PathVariable("id") @Positive(message = "global.400.003") final Long id) {
        return customerService.findCustomerById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@Valid @RequestBody final CustomerDTO customer) {
        return customerService.createCustomer(customer);
    }
}
