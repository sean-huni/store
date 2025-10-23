package com.example.store.persistence.repo;

import com.example.store.aop.performance.annotation.TrackSqlPerf;
import com.example.store.persistence.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CustomerRepo extends JpaRepository<Customer, Long> {

    @TrackSqlPerf(value = "findCustomersByNameContainingIgnoreCase",
            timeUnit = TimeUnit.MILLISECONDS,
            warnThreshold = 160,
            errorThreshold = 250,
            maxExpectedQueries = 1,
            critical = true,  // Mark as critical for production monitoring
            metricTags = {"service=store", "operation=search"}
    )
    @Query("SELECT c FROM Customer c WHERE UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%')) ORDER BY c.name")
    List<Customer> findCustomersByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    Optional<Customer> findCustomerById(@Param("id") Long id);

    /**
     * Retrieves a customer by their unique identifier, along with their associated orders.
     * The {@link #findCustomerByIdWithOrders(Long id)} addresses infamous The N+1 fetch issue.
     *
     * @param id the unique identifier of the customer to be retrieved
     * @return an {@link Optional} containing the customer if found, or an empty {@link Optional} if no customer exists with the given id
     */
    @TrackSqlPerf(value = "findCustomerByIdWithOrders",
            timeUnit = TimeUnit.MILLISECONDS,
            warnThreshold = 100,
            errorThreshold = 250,
            maxExpectedQueries = 1,
            critical = true,  // Mark as critical for production monitoring
            metricTags = {"service=store", "operation=fetch-with-orders"}
    )
    @Query(value = "from Customer c where c.id =:id")
    @EntityGraph(attributePaths = {"orders"})
    Optional<Customer> findCustomerByIdWithOrders(@Param("id") Long id);

}
