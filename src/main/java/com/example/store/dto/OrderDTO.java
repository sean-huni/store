package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDTO extends AbstractSuperDTO {
    @NotBlank(message = "global.400.004")
    private String description;
    @NotNull(message = "order.400.000")
    private Long customerId;
    private CustomerDTO customer;
    private Set<Long> productIds;
}
