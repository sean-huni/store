package com.example.store.controller.auth;

import com.example.store.dto.auth.req.AuthReqDTO;
import com.example.store.dto.auth.req.RefreshTokenReqDTO;
import com.example.store.dto.auth.req.RegReqDTO;
import com.example.store.dto.auth.resp.AuthRespDTO;
import com.example.store.service.auth.AuthService;
import com.example.store.validation.BearerToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authenticationService;

    @PostMapping("/register")
    public AuthRespDTO register(final @Valid @RequestBody RegReqDTO request) {
        return authenticationService.register(request);
    }

    @PostMapping("/authenticate")
    public AuthRespDTO authenticate(final @Valid @RequestBody AuthReqDTO request) {
        return authenticationService.authenticate(request);
    }

    @PostMapping("/refresh-token")
    public AuthRespDTO refreshToken(final @Valid @BearerToken @RequestHeader("Authorization") String authorizationHeader) {
        // Extract the token from "Bearer {token}" format (validation is handled by @BearerToken)
        final String token = authorizationHeader.substring("Bearer ".length());
        return authenticationService.refreshToken(new RefreshTokenReqDTO(token));
    }
}
