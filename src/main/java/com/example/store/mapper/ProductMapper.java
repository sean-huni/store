package com.example.store.mapper;

import com.example.store.dto.ProductDTO;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "sku", expression = "java(productDTO.getSku())")
    Product toProduct(ProductDTO productDTO);

    @Mapping(target = "orderIds", expression = "java(mapProductOrdersToOrderIds(product.getOrders()))")
    @Mapping(target = "sku", expression = "java(product.getSku())")
    ProductDTO toProductDTO(Product product);

    List<ProductDTO> toProductDTOList(List<Product> products);

    // Custom mapping methods
    default Set<Long> mapProductOrdersToOrderIds(List<ProductOrder> productOrders) {
        if (productOrders == null) {
            return Collections.emptySet();
        }
        return productOrders.stream()
                .map(po -> po.getOrder().getId())
                .collect(Collectors.toUnmodifiableSet());
    }
    
    default UUID generateSkuIfMissing(UUID sku) {
        return sku != null ? sku : UUID.randomUUID();
    }
}
