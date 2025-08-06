package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;
    private final String email = "user@example.com";
    private final String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final Long expiration = 86400000L; // 1 day
    private final Long refreshExpiration = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        // Setup common test data
        userDetails = new User(
                email,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        
        // Setup JWT properties
        when(jwtProperties.getSecretKey()).thenReturn(secretKey);
        when(jwtProperties.getExpiration()).thenReturn(expiration);
        when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);
    }

    @Test
    @DisplayName("Should generate access token with correct claims")
    void shouldGenerateAccessTokenWithCorrectClaims() {
        // When
        String token = jwtService.generateAccessToken(userDetails);
        
        // Then
        assertNotNull(token);
        assertEquals(email, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
        
        // Verify authorities claim
        List<?> authorities = jwtService.extractClaim(token, claims -> 
                claims.get("authorities", List.class));
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0));
    }

    @Test
    @DisplayName("Should generate refresh token with correct claims")
    void shouldGenerateRefreshTokenWithCorrectClaims() {
        // When
        String token = jwtService.generateRefreshToken(userDetails);
        
        // Then
        assertNotNull(token);
        assertEquals(email, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should validate token with correct user details")
    void shouldValidateTokenWithCorrectUserDetails() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);
        
        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should not validate token with incorrect user details")
    void shouldNotValidateTokenWithIncorrectUserDetails() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails wrongUser = new User(
                "wrong@example.com",
                "password",
                Collections.emptyList()
        );
        
        // When
        boolean isValid = jwtService.isTokenValid(token, wrongUser);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should not validate expired token")
    void shouldNotValidateExpiredToken() {
        // Given
        // Mock a very short expiration time
        when(jwtProperties.getExpiration()).thenReturn(1L); // 1 millisecond
        String token = jwtService.generateAccessToken(userDetails);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);
        
        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpirationDateFromToken() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);
        
        // When
        Date expiration = jwtService.extractClaim(token, claims -> claims.getExpiration());
        
        // Then
        assertNotNull(expiration);
        // The expiration should be roughly now + expiration time
        long expectedExpiration = System.currentTimeMillis() + this.expiration;
        // Allow for a 10-second margin of error
        assertTrue(Math.abs(expectedExpiration - expiration.getTime()) < 10000);
    }
}