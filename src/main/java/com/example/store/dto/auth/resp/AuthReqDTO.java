package com.example.store.dto.auth.resp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthReqDTO {
    
    @Email(message = "auth.400.000")
    @NotBlank(message = "auth.400.001")
    private String email;
    
    @NotBlank(message = "auth.400.002")
    private String password;
}