package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<Order, Long> {

}
