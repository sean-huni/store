package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
}
