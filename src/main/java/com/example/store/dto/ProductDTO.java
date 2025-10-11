package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends AbstractSuperDTO {
    @NotBlank(message = "product.400.000")
    private String description;
    @NotNull(message = "product.400.001")
    private UUID sku;
    private Set<Long> orderIds;
}
