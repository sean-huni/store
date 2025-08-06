package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class ProductDTO extends AbstractSuperDTO {
    @NotBlank(message = "product.400.000")
    private String description;
    @jakarta.validation.constraints.NotNull(message = "product.400.001")
    private UUID sku;
    private Set<Long> orderIds;
}
