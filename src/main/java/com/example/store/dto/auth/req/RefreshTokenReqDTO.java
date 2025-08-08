package com.example.store.dto.auth.req;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenReqDTO(
        @NotBlank(message = "global.400.008")
        String refreshToken
) {
}