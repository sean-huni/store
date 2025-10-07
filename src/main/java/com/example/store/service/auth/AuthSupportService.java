package com.example.store.service.auth;

import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSupportService {
    private final UserRegistrationService userRegistrationService;
    private final TokenRefreshService tokenRefreshService;

    public AuthRespDTO register(final RegReqDTO request) {
        return userRegistrationService.register(request);
    }

    public AuthRespDTO refreshToken(final RefreshTokenReqDTO request) {
        return tokenRefreshService.refreshToken(request);
    }
}