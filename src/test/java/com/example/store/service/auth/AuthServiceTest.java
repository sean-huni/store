package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.persistence.entity.Role;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - {Unit}")
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

    @Mock
    private MessageSource messageSource;

    @Mock
    private AuthSupportService authSupportService;

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
        regReqDTO = new RegReqDTO("John", "Doe", email, password);
        authReqDTO = new AuthReqDTO(email, password);

        refreshTokenReqDTO = new RefreshTokenReqDTO(refreshToken);

        user = User.builder()
                .email(email)
                .password(encodedPassword)
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Setup JWT properties with lenient stubbing to avoid unnecessary stubbing warnings
        lenient().when(jwtProperties.getExpiration()).thenReturn(expiration);

        // Setup MessageSource with lenient stubbing for common messages
        lenient().when(messageSource.getMessage(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object[] args = invocation.getArgument(1);
                    String defaultMessage = invocation.getArgument(2);

                    if ("auth.400.008".equals(key)) {
                        return "Invalid email or password";
                    } else if ("auth.400.009".equals(key)) {
                        return "User not found";
                    } else if ("auth.400.011".equals(key) && args != null && args.length > 0) {
                        return "Email already registered: " + args[0];
                    } else {
                        return defaultMessage;
                    }
                });
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        AuthRespDTO expectedResponse = AuthRespDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .build();
        when(authSupportService.register(regReqDTO)).thenReturn(expectedResponse);

        // When
        AuthRespDTO response = authService.register(regReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(expiration, response.expiresIn());

        // Verify the authSupportService.register was called
        verify(authSupportService).register(regReqDTO);
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Given
        when(authSupportService.register(regReqDTO)).thenThrow(new EmailAlreadyExistsException("auth.400.011", new String[]{email}));

        // When/Then
        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(regReqDTO)
        );

        assertEquals("auth.400.011", exception.getMessage());
        verify(authSupportService).register(regReqDTO);
    }

    @Test
    @DisplayName("Should authenticate user successfully")
    void shouldAuthenticateUserSuccessfully() {
        // Given
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
        when(authenticationManager.authenticate(authToken)).thenReturn(authenticatedToken);
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);
        when(jwtService.getAccessTokenExpiration()).thenReturn(expiration);

        // When
        AuthRespDTO response = authService.authenticate(authReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(expiration, response.expiresIn());

        // Verify authentication
        verify(authenticationManager).authenticate(authToken);
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
        final UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
        when(authenticationManager.authenticate(authToken)).thenThrow(new UsernameNotFoundException("User not found"));

        // When/Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.authenticate(authReqDTO)
        );

        assertEquals("User not found", exception.getMessage());
        verify(authenticationManager).authenticate(authToken);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        AuthRespDTO expectedResponse = AuthRespDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .build();
        when(authSupportService.refreshToken(refreshTokenReqDTO)).thenReturn(expectedResponse);

        // When
        AuthRespDTO response = authService.refreshToken(refreshTokenReqDTO);

        // Then
        assertNotNull(response);
        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(expiration, response.expiresIn());

        // Verify the authSupportService.refreshToken was called
        verify(authSupportService).refreshToken(refreshTokenReqDTO);
    }

    @Test
    @DisplayName("Should throw exception when refreshing with invalid token")
    void shouldThrowExceptionWhenRefreshingWithInvalidToken() {
        // Given
        when(authSupportService.refreshToken(refreshTokenReqDTO)).thenThrow(new InvalidRefreshTokenException("auth.400.007"));

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("auth.400.007", exception.getMessage());
        verify(authSupportService).refreshToken(refreshTokenReqDTO);
    }

    @Test
    @DisplayName("Should throw exception when refreshing with null username")
    void shouldThrowExceptionWhenRefreshingWithNullUsername() {
        // Given
        when(authSupportService.refreshToken(refreshTokenReqDTO)).thenThrow(new InvalidRefreshTokenException("auth.400.006"));

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("auth.400.006", exception.getMessage());
        verify(authSupportService).refreshToken(refreshTokenReqDTO);
    }

    @Test
    @DisplayName("Should throw exception when refreshing with non-existent user")
    void shouldThrowExceptionWhenRefreshingWithNonExistentUser() {
        // Given
        final String expectedMessage = messageSource.getMessage("auth.400.009", null, "User not found", Locale.getDefault());
        when(authSupportService.refreshToken(refreshTokenReqDTO)).thenThrow(new UsernameNotFoundException(expectedMessage));

        // When/Then
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        // Verify the exception message matches what would be returned by the MessageSource
        assertEquals(expectedMessage, exception.getMessage());
        verify(authSupportService).refreshToken(refreshTokenReqDTO);
    }

    @Test
    @DisplayName("Should handle general exception during token refresh")
    void shouldHandleGeneralExceptionDuringTokenRefresh() {
        // Given
        when(authSupportService.refreshToken(refreshTokenReqDTO)).thenThrow(new InvalidRefreshTokenException("auth.400.006"));

        // When/Then
        InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refreshToken(refreshTokenReqDTO)
        );

        assertEquals("auth.400.006", exception.getMessage());
        verify(authSupportService).refreshToken(refreshTokenReqDTO);
    }
}