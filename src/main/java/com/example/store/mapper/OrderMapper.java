package com.example.store.mapper;

import com.example.store.dto.OrderDTO;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.ProductOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "products", ignore = true)
    Order toOrder(OrderDTO orderDTO);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "productIds", expression = "java(mapProductOrdersToProductIds(order.getProducts()))")
    OrderDTO toOrderDTO(Order order);

    List<OrderDTO> ordersToOrderDTOs(List<Order> orders);

    // Custom mapping methods
    default Set<Long> mapProductOrdersToProductIds(List<ProductOrder> productOrders) {
        if (productOrders == null) {
            return null;
        }

        return productOrders.stream()
                .filter(po -> po.getProduct() != null)
                .map(po -> po.getProduct())
                .filter(product -> product.getId() != null)
                .map(product -> product.getId())
                .collect(Collectors.toUnmodifiableSet());
    }
}
