package com.example.store.persistence.repo;

import com.example.store.aop.performance.annotation.TrackSqlPerf;
import com.example.store.persistence.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ProductOrderRepo extends JpaRepository<ProductOrder, Long> {

    List<ProductOrder> findProductOrdersByProduct_Id(Long productId);

    @TrackSqlPerf(value = "findOrderIdsByProduct_Id",
            timeUnit = TimeUnit.MILLISECONDS,
            warnThreshold = 10,
            errorThreshold = 20,
            maxExpectedQueries = 1,
            critical = true,  // Mark as critical for production monitoring
            metricTags = {"service=store", "operation=fetch-product-order-by-product-id"}
    )
    @Query("SELECT po.order.id FROM ProductOrder po WHERE po.product.id = :productId ORDER BY po.order.id")
    Set<Long> findOrderIdsByProduct_Id(Long productId);
}
