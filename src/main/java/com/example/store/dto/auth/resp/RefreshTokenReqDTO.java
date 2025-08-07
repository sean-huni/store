package com.example.store.dto.auth.resp;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenReqDTO {
    
    @NotBlank(message = "{global.400.refresh.token.required}")
    private String refreshToken;
}