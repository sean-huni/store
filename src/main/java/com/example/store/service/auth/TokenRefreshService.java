package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.persistence.entity.User;
import com.example.store.persistence.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MessageSource messageSource;
    private final String TOKEN_TYPE = "Bearer";

    @Transactional(readOnly = true)
    public AuthRespDTO refreshToken(final RefreshTokenReqDTO request) {
        final String refreshToken = request.refreshToken();

        try {
            final String userEmail = jwtService.extractUsername(refreshToken);

            if (userEmail == null) {
                throw new InvalidRefreshTokenException("auth.400.006");
            }

            final String userNotFoundMessage = messageSource.getMessage("auth.400.009", null, "User not found", Locale.getDefault());
            final User user = userRepo.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("Error refreshing token: User not found");
                        return new UsernameNotFoundException(userNotFoundMessage);
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