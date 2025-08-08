package com.example.store.dto.auth.resp;

import com.google.gson.annotations.SerializedName;

public record AuthRespDTO(
    @SerializedName("access_token")
    String accessToken,
    
    @SerializedName("refresh_token")
    String refreshToken,
    
    @SerializedName("token_type")
    String tokenType,
    
    @SerializedName("expires_in")
    Long expiresIn
) {
    // Static builder method to maintain compatibility with existing code
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthRespDTO build() {
            return new AuthRespDTO(accessToken, refreshToken, tokenType, expiresIn);
        }
    }
}
