package com.example.store.service.store.impl;

import com.example.store.dto.CustomerDTO;
import com.example.store.mapper.CustomerMapper;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.repo.CustomerRepo;
import com.example.store.service.store.CustomerService;
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
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepo customerRepo;
    private final CustomerMapper customerMapper;

    @Cacheable(value = "customers", key = "'all_page_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public List<CustomerDTO> findAllCustomers(final Pageable pageable) {
        final Page<Customer> customerPage = customerRepo.findAll(pageable);
        return customerMapper.toCustomerDTOs(customerPage.getContent());
    }

    @Cacheable(value = "customers", key = "'name_' + #name + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public List<CustomerDTO> findCustomersNameContainingSubString(final String name, Pageable pageable) {
        return customerMapper.toCustomerDTOs(customerRepo.findCustomersByNameContainingIgnoreCase(name, pageable));
    }

    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDTO createCustomer(final CustomerDTO customerDTO) {
        final var customer = customerMapper.toCustomer(customerDTO);
        // Set created and updated times
        ZonedDateTime now = ZonedDateTime.now();
        customer.setCreated(now);
        customer.setUpdated(now);
        final var savedCustomer = customerRepo.save(customer);
        return customerMapper.toCustomerDTO(savedCustomer);
    }

    @Override
    public CustomerDTO findCustomerById(final Long id) {
        final var optCustomer = customerRepo.findById(id);
        return optCustomer.map(customerMapper::toCustomerDTO).orElse(null);
    }
}
