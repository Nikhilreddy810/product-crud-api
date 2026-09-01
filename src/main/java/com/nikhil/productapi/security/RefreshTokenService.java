package com.nikhil.productapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Refresh tokens are stored in Redis keyed by tokenId (jti) -> username.
 * Rotation: old tokenId is deleted and a new one is stored on every /refresh call.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public void store(String tokenId, String username) {
        redisTemplate.opsForValue().set(
                PREFIX + tokenId,
                username,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMs())
        );
    }

    public boolean isValid(String tokenId, String username) {
        String stored = redisTemplate.opsForValue().get(PREFIX + tokenId);
        return stored != null && stored.equals(username);
    }

    public void revoke(String tokenId) {
        redisTemplate.delete(PREFIX + tokenId);
    }
}
