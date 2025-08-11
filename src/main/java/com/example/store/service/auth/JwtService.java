package com.example.store.service.auth;

import com.example.store.config.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(final UserDetails userDetails) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return createToken(claims, userDetails.getUsername(), jwtProperties.getExpiration());
    }

    public String generateRefreshToken(final UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), jwtProperties.getRefreshExpiration());
    }

    private String createToken(final Map<String, Object> claims, final String subject, final Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    public Boolean isTokenValid(final String token, final UserDetails userDetails) {
        if (token == null || token.isEmpty() || userDetails == null) {
            return false;
        }

        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) &&
                    !isTokenExpired(token) &&
                    userDetails.isEnabled();
        } catch (final ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage(), e);
            return false;
        } catch (final MalformedJwtException e) {
            log.debug("Malformed JWT: {}", e.getMessage(), e);
            return false;
        } catch (final IllegalArgumentException e) {
            log.debug("Invalid token: {}", e.getMessage(), e);
            return false;
        }
    }

    private Boolean isTokenExpired(final String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(final String token) throws ExpiredJwtException {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        final byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
