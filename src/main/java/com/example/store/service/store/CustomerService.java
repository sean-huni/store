package com.example.store.service.store;

import com.example.store.dto.CustomerDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    List<CustomerDTO> findAllCustomers(Pageable pageable);

    List<CustomerDTO> findCustomersNameContainingSubString(String name, Pageable pageable);

    CustomerDTO createCustomer(CustomerDTO customer);

    CustomerDTO findCustomerById(Long id);
}
