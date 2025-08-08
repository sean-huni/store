package com.example.store.dto.auth.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthReqDTO(
    @Email(message = "auth.400.000")
    @NotBlank(message = "auth.400.001")
    String email,
    
    @NotBlank(message = "auth.400.002")
    String password
) {
}