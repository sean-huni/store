package com.example.store.dto.auth.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegReqDTO(
    @NotBlank(message = "auth.400.003")
    String firstName,
    
    @NotBlank(message = "auth.400.004")
    String lastName,
    
    @Email(message = "auth.400.000")
    @NotBlank(message = "auth.400.001")
    String email,
    
    @NotBlank(message = "auth.400.002")
    @Size(min = 8, message = "auth.400.005")
    String password
) {
}