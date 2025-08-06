package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Validated
public class CustomerDTO extends AbstractSuperDTO {
    @NotBlank(message = "global.400.001")
    private String name;
    private Set<Long> orders;
}
