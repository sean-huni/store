package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.io.DecodingException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Edge Case Tests")
class JwtServiceEdgeCaseTest {

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
        // Use lenient stubbing for all properties as they're not used in all tests
        lenient().when(jwtProperties.getSecretKey()).thenReturn(secretKey);
        lenient().when(jwtProperties.getExpiration()).thenReturn(expiration);
        lenient().when(jwtProperties.getRefreshExpiration()).thenReturn(refreshExpiration);
    }

    @Test
    @DisplayName("Should throw exception when token is null in extractUsername")
    void shouldThrowExceptionWhenTokenIsNullInExtractUsername() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractUsername(null));
    }

    @Test
    @DisplayName("Should throw exception when token is empty in extractUsername")
    void shouldThrowExceptionWhenTokenIsEmptyInExtractUsername() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractUsername(""));
    }

    @Test
    @DisplayName("Should throw exception when token is null in extractClaim")
    void shouldThrowExceptionWhenTokenIsNullInExtractClaim() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractClaim(null, claims -> claims.getSubject()));
    }

    @Test
    @DisplayName("Should throw exception when token is empty in extractClaim")
    void shouldThrowExceptionWhenTokenIsEmptyInExtractClaim() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> jwtService.extractClaim("", claims -> claims.getSubject()));
    }

    @Test
    @DisplayName("Should return false when token is null in isTokenValid")
    void shouldReturnFalseWhenTokenIsNullInIsTokenValid() {
        // When/Then
        assertFalse(jwtService.isTokenValid(null, userDetails));
    }

    @Test
    @DisplayName("Should return false when token is empty in isTokenValid")
    void shouldReturnFalseWhenTokenIsEmptyInIsTokenValid() {
        // When/Then
        assertFalse(jwtService.isTokenValid("", userDetails));
    }

    @Test
    @DisplayName("Should return false when userDetails is null in isTokenValid")
    void shouldReturnFalseWhenUserDetailsIsNullInIsTokenValid() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);

        // When/Then
        assertFalse(jwtService.isTokenValid(token, null));
    }

    @Test
    @DisplayName("Should handle ExpiredJwtException when extracting claims from expired token")
    void shouldHandleExpiredJwtExceptionWhenExtractingClaimsFromExpiredToken() {
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
        // This should throw ExpiredJwtException when trying to extract claims
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractClaim(expiredToken, claims -> claims.getSubject()));
    }

    @Test
    @DisplayName("Should return false for isTokenValid when username doesn't match")
    void shouldReturnFalseForIsTokenValidWhenUsernameDoesntMatch() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails differentUser = new User(
                "different@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When/Then
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }

    @Test
    @DisplayName("Should return false for isTokenValid when user is disabled")
    void shouldReturnFalseForIsTokenValidWhenUserIsDisabled() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails disabledUser = new User(
                email,
                "password",
                false, // disabled
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // When/Then
        assertFalse(jwtService.isTokenValid(token, disabledUser));
    }

    @Test
    @DisplayName("Should return false for isTokenValid when MalformedJwtException is thrown")
    void shouldReturnFalseForIsTokenValidWhenMalformedJwtExceptionIsThrown() {
        // Given
        String token = "malformed.token";

        // When/Then
        // Update JwtService to catch MalformedJwtException
        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should return false for isTokenValid when IllegalArgumentException is thrown")
    void shouldReturnFalseForIsTokenValidWhenIllegalArgumentExceptionIsThrown() {
        // Given
        // Create a token that will cause an IllegalArgumentException
        // This happens when the token is not null or empty, but is invalid in a way
        // that causes an IllegalArgumentException to be thrown during processing
        String token = "invalid-token-format";

        // When/Then
        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should handle invalid secret key")
    void shouldHandleInvalidSecretKey() {
        // Given
        when(jwtProperties.getSecretKey()).thenReturn("invalid-key"); // Not a valid Base64 key

        // When/Then
        assertThrows(DecodingException.class, () -> jwtService.generateAccessToken(userDetails));
    }

    @Test
    @DisplayName("Should verify getSignInKey works with valid key")
    void shouldVerifyGetSignInKeyWorksWithValidKey() {
        // Given
        // Valid key is already set up in setUp()

        // When/Then
        // This will indirectly test getSignInKey by generating a token
        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);
    }

    @Test
    @DisplayName("Should test extractClaim with various claim types")
    void shouldTestExtractClaimWithVariousClaimTypes() {
        // Given
        String token = jwtService.generateAccessToken(userDetails);

        // When/Then
        // Extract and verify subject claim
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertEquals(email, subject);

        // Extract and verify expiration claim
        Date expirationDate = jwtService.extractClaim(token, Claims::getExpiration);
        assertNotNull(expirationDate);

        // Extract and verify issuedAt claim
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
        assertNotNull(issuedAt);

        // Verify the token is valid
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Should handle additional exception types in isTokenValid")
    void shouldHandleAdditionalExceptionTypesInIsTokenValid() {
        // 1. Test with a token that will cause a MalformedJwtException
        assertFalse(jwtService.isTokenValid("malformed.jwt.token", userDetails));

        // 2. Test with a token that will cause an IllegalArgumentException
        assertFalse(jwtService.isTokenValid("not-even-a-jwt", userDetails));
    }
}