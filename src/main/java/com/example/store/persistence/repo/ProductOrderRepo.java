package com.example.store.persistence.repo;

import com.example.store.persistence.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ProductOrderRepo extends JpaRepository<ProductOrder, Long> {

    List<ProductOrder> findProductOrdersByProduct_Id(Long productId);

    @Query("SELECT po.order.id FROM ProductOrder po WHERE po.product.id = :productId")
    Set<Long> findOrderIdsByProduct_Id(Long productId);
}
