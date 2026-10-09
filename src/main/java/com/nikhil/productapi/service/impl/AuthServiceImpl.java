package com.nikhil.productapi.service.impl;

import com.nikhil.productapi.dto.AuthRequest;
import com.nikhil.productapi.dto.AuthResponse;
import com.nikhil.productapi.dto.RefreshRequest;
import com.nikhil.productapi.entity.AppUser;
import com.nikhil.productapi.exception.BadRequestException;
import com.nikhil.productapi.repository.AppUserRepository;
import com.nikhil.productapi.security.JwtUtil;
import com.nikhil.productapi.security.RefreshTokenService;
import com.nikhil.productapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return issueTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();
        String username;
        String tokenId;
        try {
            username = jwtUtil.extractUsername(token);
            tokenId = jwtUtil.extractTokenId(token);
        } catch (Exception e) {
            throw new BadRequestException("Invalid refresh token");
        }

        if (jwtUtil.isTokenExpired(token) || !refreshTokenService.isValid(tokenId, username)) {
            throw new BadRequestException("Refresh token expired or revoked");
        }

        // rotation: revoke old, issue new
        refreshTokenService.revoke(tokenId);

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User no longer exists"));

        return issueTokens(user);
    }

    @Override
    public void logout(RefreshRequest request) {
        try {
            String tokenId = jwtUtil.extractTokenId(request.getRefreshToken());
            refreshTokenService.revoke(tokenId);
        } catch (Exception ignored) {
            // already invalid/expired, nothing to revoke
        }
    }

    private AuthResponse issueTokens(AppUser user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        String tokenId = jwtUtil.extractTokenId(refreshToken);

        refreshTokenService.store(tokenId, user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
