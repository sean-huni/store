package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import com.example.store.dto.auth.req.AuthRespDTO;
import com.example.store.dto.auth.resp.AuthReqDTO;
import com.example.store.dto.auth.resp.RefreshTokenReqDTO;
import com.example.store.dto.auth.resp.RegReqDTO;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
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
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        
        // Create new user
        final User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
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
        log.info("Authenticating user: {}", request.getEmail());
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
        
        User user = userRepo.findByEmail(request.getEmail())
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
        String refreshToken = request.getRefreshToken();
        
        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            
            if (userEmail == null) {
                throw new InvalidRefreshTokenException("Invalid refresh token");
            }
            
            User user = userRepo.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("Error refreshing token: User not found");
                        return new UsernameNotFoundException("User not found");
                    });
            
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new InvalidRefreshTokenException("Invalid or expired refresh token");
            }
            
            String newAccessToken = jwtService.generateAccessToken(user);
            
            log.info("Token refreshed for user: {}", user.getEmail());
            
            return AuthRespDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType(TOKEN_TYPE)
                    .expiresIn(jwtProperties.getExpiration())
                    .build();

        } catch (UsernameNotFoundException e) {
            // Rethrow UsernameNotFoundException to maintain the expected exception type
            throw e;
        } catch (InvalidRefreshTokenException e) {
            // Rethrow InvalidRefreshTokenException to maintain the original error message
            log.error("Error refreshing token: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }
}