package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepo extends JpaRepository<Customer, Long> {

    List<Customer> findCustomersByNameContainingIgnoreCase(String name, Pageable pageable);

}
