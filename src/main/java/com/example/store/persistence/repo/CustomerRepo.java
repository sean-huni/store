package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepo extends JpaRepository<Customer, Long> {

    List<Customer> findCustomersByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Retrieves a customer by their unique identifier, along with their associated orders.
     * The {@link #findCustomerById(Long id)} addresses infamous The N+1 fetch issue.
     *
     * @param id the unique identifier of the customer to be retrieved
     * @return an {@link Optional} containing the customer if found, or an empty {@link Optional} if no customer exists with the given id
     */
    @Query(value = "from Customer c where c.id =:id")
    @EntityGraph(attributePaths = {"orders"})
    Optional<Customer> findCustomerById(@Param("id") Long id);

}
