package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import com.example.store.dto.auth.AuthReqDTO;
import com.example.store.dto.auth.AuthRespDTO;
import com.example.store.dto.auth.RefreshTokenReqDTO;
import com.example.store.dto.auth.RegReqDTO;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private RegReqDTO regReqDTO;
    private AuthReqDTO authReqDTO;
    private RefreshTokenReqDTO refreshTokenReqDTO;
    private User user;
    private final String email = "user@example.com";
    private final String password = "password";
    private final String encodedPassword = "encodedPassword";
    private final String accessToken = "access.token.value";
    private final String refreshToken = "refresh.token.value";
    private final Long expiration = 3600L;

    @BeforeEach
    void setUp() {
        // Setup common test data
        regReqDTO = new RegReqDTO();
        regReqDTO.setFirstName("John");
        regReqDTO.setLastName("Doe");
        regReqDTO.setEmail(email);
        regReqDTO.setPassword(password);

        authReqDTO = new AuthReqDTO();
        authReqDTO.setEmail(email);
        authReqDTO.setPassword(password);

        refreshTokenReqDTO = new RefreshTokenReqDTO(refreshToken);

        user = User.builder()
                .email(email)
                .password(encodedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Setup JWT properties
        when(jwtProperties.getExpiration()).thenReturn(expiration);
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepo.save(any(User.class))).thenReturn(user);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn(refreshToken);

        // When
        AuthRespDTO response = authService.register(regReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(expiration, response.getExpiresIn());

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(email, savedUser.getEmail());
        assertEquals(encodedPassword, savedUser.getPassword());
        assertEquals("John", savedUser.getFirstName());
        assertEquals("Doe", savedUser.getLastName());
        assertEquals(User.Role.USER, savedUser.getRole());
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Given
        when(userRepo.existsByEmail(email)).thenReturn(true);

        // When/Then
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(regReqDTO)
        );

        assertEquals("Email already registered: " + email, exception.getMessage());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate user successfully")
    void shouldAuthenticateUserSuccessfully() {
        // Given
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        // When
        AuthRespDTO response = authService.authenticate(authReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(expiration, response.getExpiresIn());

        // Verify authentication
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
    }

    @Test
    @DisplayName("Should throw exception when authenticating with bad credentials")
    void shouldThrowExceptionWhenAuthenticatingWithBadCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        // When/Then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.authenticate(authReqDTO)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when authenticating non-existent user")
    void shouldThrowExceptionWhenAuthenticatingNonExistentUser() {
        // Given
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.authenticate(authReqDTO)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(refreshToken, user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);

        // When
        AuthRespDTO response = authService.refreshToken(refreshTokenReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.getAccessToken());
        assertEquals(refreshToken, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(expiration, response.getExpiresIn());
    }

    @Test
    @DisplayName("Should throw exception when refreshing with invalid token")
    void shouldThrowExceptionWhenRefreshingWithInvalidToken() {
        // Given
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(refreshToken, user)).thenReturn(false);

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when refreshing with null username")
    void shouldThrowExceptionWhenRefreshingWithNullUsername() {
        // Given
        when(jwtService.extractUsername(refreshToken)).thenReturn(null);

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when refreshing with non-existent user")
    void shouldThrowExceptionWhenRefreshingWithNonExistentUser() {
        // Given
        when(jwtService.extractUsername(refreshToken)).thenReturn(email);
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle general exception during token refresh")
    void shouldHandleGeneralExceptionDuringTokenRefresh() {
        // Given
        when(jwtService.extractUsername(refreshToken)).thenThrow(new RuntimeException("Some error"));

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }
}