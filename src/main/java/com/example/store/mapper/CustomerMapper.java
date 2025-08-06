package com.example.store.mapper;

import com.example.store.dto.CustomerDTO;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    
    @Mapping(target = "orders", ignore = true)
    Customer toCustomer(final CustomerDTO customerDTO);

    @Mapping(target = "orders", expression = "java(mapOrdersToIds(customer.getOrders()))")
    CustomerDTO toCustomerDTO(Customer customer);

    List<CustomerDTO> toCustomerDTOs(List<Customer> customer);
    
    default Set<Long> mapOrdersToIds(Set<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream()
                .map(Order::getId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
