package com.example.store.service.auth;

import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.persistence.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthSupportService authSupportService;

    public AuthRespDTO register(final RegReqDTO request) {
        return authSupportService.register(request);
    }

    @Transactional(readOnly = true)
    public AuthRespDTO authenticate(final AuthReqDTO request) {
        log.info("🔐 Authenticating user: {}", request.email());

        try {
            // Authenticate using Spring's AuthenticationManager
            // This will use our CustomUserDetailsService to load the user and validate credentials
            final var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            // Get the authenticated User from the authentication result
            // This avoids the redundant database lookup since CustomUserDetailsService already loaded it
            final User user = (User) authentication.getPrincipal();

            final String accessToken = jwtService.generateAccessToken(user);
            final String refreshToken = jwtService.generateRefreshToken(user);

            log.info("🔐 User authenticated successfully: {}", user.getEmail());

            return AuthRespDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType(TOKEN_TYPE)
                    .expiresIn(jwtService.getAccessTokenExpiration())
                    .build();

        } catch (final BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.email(), e);
            throw new BadCredentialsException("auth.400.008");
        }
    }

    public AuthRespDTO refreshToken(final RefreshTokenReqDTO request) {
        return authSupportService.refreshToken(request);
    }
}