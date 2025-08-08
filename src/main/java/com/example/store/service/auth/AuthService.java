package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final String TOKEN_TYPE = "Bearer";

    @Transactional
    public AuthRespDTO register(final RegReqDTO request) {
        log.info("Registering new user with email: {}", request.email());

        // Check if user already exists
        if (userRepo.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered: %s".formatted(request.email()));
        }

        // Create new user
        final User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepo.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User registered successfully: {}", user.getEmail());

        return AuthRespDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtProperties.getExpiration())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthRespDTO authenticate(final AuthReqDTO request) {
        log.info("Authenticating user: {}", request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepo.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("User authenticated successfully: {}", user.getEmail());

        return AuthRespDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtProperties.getExpiration())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthRespDTO refreshToken(final RefreshTokenReqDTO request) {
        final String refreshToken = request.refreshToken();

        try {
            final String userEmail = jwtService.extractUsername(refreshToken);

            if (userEmail == null) {
                throw new InvalidRefreshTokenException("auth.400.006");
            }

            final User user = userRepo.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("Error refreshing token: User not found");
                        return new UsernameNotFoundException("User not found");
                    });

            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new InvalidRefreshTokenException("auth.400.007");
            }

            String newAccessToken = jwtService.generateAccessToken(user);

            log.info("Token refreshed for user: {}", user.getEmail());

            return AuthRespDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType(TOKEN_TYPE)
                    .expiresIn(jwtProperties.getExpiration())
                    .build();

        } catch (final UsernameNotFoundException e) {
            log.error("Invalid refresh token: {}", refreshToken, e);
            // Rethrow UsernameNotFoundException to maintain the expected exception type
            throw e;
        } catch (final InvalidRefreshTokenException e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            // Rethrow InvalidRefreshTokenException to maintain the original error message
            throw e;
        } catch (final Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new InvalidRefreshTokenException("auth.400.006");
        }
    }
}