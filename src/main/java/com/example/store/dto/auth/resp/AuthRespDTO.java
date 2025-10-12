package com.example.store.dto.auth.resp;

public record AuthRespDTO(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {
    // Static builder method to maintain compatibility with existing code
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;

        public Builder accessToken(final String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(final String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(final String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(final Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthRespDTO build() {
            return new AuthRespDTO(accessToken, refreshToken, tokenType, expiresIn);
        }
    }
}
