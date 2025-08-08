package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomerDTO extends AbstractSuperDTO {
    @NotBlank(message = "global.400.001")
    private String name;
    private Set<Long> orders;
}
