package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
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
        // Use lenient stubbing for expiration and refresh expiration as they're not used in all tests
        lenient().when(jwtProperties.getExpiration()).thenReturn(expiration);
        lenient().when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);
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

    @Test
    @DisplayName("Should handle malformed tokens")
    void shouldHandleMalformedTokens() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When/Then
        // Expect an exception when extracting username from a malformed token
        assertThrows(Exception.class, () -> jwtService.extractUsername(malformedToken));

        // This test verifies that the exception is thrown, which is what we want
        // The code coverage will show that the exception path is exercised
    }

    @Test
    @DisplayName("Should handle tokens with invalid signatures")
    void shouldHandleTokensWithInvalidSignatures() {
        // Given
        // Generate a token with the correct user but tamper with it
        String token = jwtService.generateAccessToken(userDetails);
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalid_signature";

        // When/Then
        // Expect an exception when extracting username from a token with invalid signature
        assertThrows(Exception.class, () -> jwtService.extractUsername(tamperedToken));

        // This test verifies that the exception is thrown, which is what we want
        // The code coverage will show that the exception path is exercised
    }

    @Test
    @DisplayName("Should handle extracting missing claims")
    void shouldHandleExtractingMissingClaims() {
        // Given
        String token = jwtService.generateRefreshToken(userDetails);

        // When/Then
        // The refresh token doesn't have authorities, so this should return null
        assertNull(jwtService.extractClaim(token, claims -> claims.get("non_existent_claim", String.class)));
    }

    @Test
    @DisplayName("Should handle ExpiredJwtException in isTokenValid method")
    void shouldHandleExpiredJwtExceptionInIsTokenValid() {
        // Given
        // Create a token that's already expired
        when(jwtProperties.getExpiration()).thenReturn(1L); // 1 millisecond
        String expiredToken = jwtService.generateAccessToken(userDetails);

        // Wait to ensure token is expired
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When/Then
        // This should trigger the ExpiredJwtException catch block in isTokenValid
        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    @DisplayName("Should handle ExpiredJwtException in isTokenExpired method")
    void shouldHandleExpiredJwtExceptionInIsTokenExpired() {
        // Given
        // Create a token that's already expired
        when(jwtProperties.getExpiration()).thenReturn(1L); // 1 millisecond
        String expiredToken = jwtService.generateAccessToken(userDetails);

        // Wait to ensure token is expired
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When/Then
        // This should trigger the ExpiredJwtException catch block in isTokenExpired
        // We can't call isTokenExpired directly as it's private, but we can call isTokenValid
        // which will call isTokenExpired internally
        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));

        // To ensure we're testing the specific branch in isTokenExpired, we'll also
        // try to extract the expiration date, which should throw an exception
        assertThrows(Exception.class, () -> jwtService.extractClaim(expiredToken, claims -> claims.getExpiration()));
    }
}