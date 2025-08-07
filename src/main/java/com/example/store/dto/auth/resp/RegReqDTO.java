package com.example.store.dto.auth.resp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegReqDTO {
    
    @NotBlank(message = "auth.400.003")
    private String firstName;
    @NotBlank(message = "auth.400.004")
    private String lastName;
    @Email(message = "auth.400.000")
    @NotBlank(message = "auth.400.001")
    private String email;
    @NotBlank(message = "auth.400.002")
    @Size(min = 8, message = "auth.400.005")
    private String password;
}