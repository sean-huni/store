package com.example.store.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.security.jwt")
public class JwtProperties {
    private String secretKey;
    private Long expiration;
    private Long refreshExpiration;
    private String tokenPrefix = "Bearer ";
    private String headerString = "Authorization";
}