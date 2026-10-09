package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.AuthRequest;
import com.nikhil.productapi.dto.AuthResponse;
import com.nikhil.productapi.dto.RefreshRequest;
import com.nikhil.productapi.entity.AppUser;
import com.nikhil.productapi.entity.Role;
import com.nikhil.productapi.exception.BadRequestException;
import com.nikhil.productapi.repository.AppUserRepository;
import com.nikhil.productapi.security.JwtUtil;
import com.nikhil.productapi.security.RefreshTokenService;
import com.nikhil.productapi.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .id(1)
                .username("admin")
                .password("encoded")
                .role(Role.ROLE_ADMIN)
                .build();
    }

    @Test
    void login_returnsTokens_whenCredentialsValid() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("admin", "ROLE_ADMIN")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("admin")).thenReturn("refresh-token");
        when(jwtUtil.extractTokenId("refresh-token")).thenReturn("jti-123");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(authenticationManager, times(1)).authenticate(any());
        verify(refreshTokenService, times(1)).store("jti-123", "admin");
    }

    @Test
    void login_throws_whenAuthenticationFails() {
        AuthRequest request = new AuthRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        doThrow(new BadCredentialsException("bad creds")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refresh_rotatesToken_whenValid() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh-token");

        when(jwtUtil.extractUsername("old-refresh-token")).thenReturn("admin");
        when(jwtUtil.extractTokenId("old-refresh-token")).thenReturn("old-jti");
        when(jwtUtil.isTokenExpired("old-refresh-token")).thenReturn(false);
        when(refreshTokenService.isValid("old-jti", "admin")).thenReturn(true);
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("admin", "ROLE_ADMIN")).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken("admin")).thenReturn("new-refresh");
        when(jwtUtil.extractTokenId("new-refresh")).thenReturn("new-jti");

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService, times(1)).revoke("old-jti");
        verify(refreshTokenService, times(1)).store("new-jti", "admin");
    }

    @Test
    void refresh_throws_whenTokenRevokedOrExpired() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("stale-token");

        when(jwtUtil.extractUsername("stale-token")).thenReturn("admin");
        when(jwtUtil.extractTokenId("stale-token")).thenReturn("stale-jti");
        when(jwtUtil.isTokenExpired("stale-token")).thenReturn(false);
        when(refreshTokenService.isValid("stale-jti", "admin")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadRequestException.class);

        verify(refreshTokenService, never()).revoke(anyString());
    }

    @Test
    void refresh_throws_whenTokenUnparseable() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("garbage-token");

        when(jwtUtil.extractUsername("garbage-token")).thenThrow(new RuntimeException("malformed"));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void logout_revokesToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("some-token");

        when(jwtUtil.extractTokenId("some-token")).thenReturn("jti-999");

        authService.logout(request);

        verify(refreshTokenService, times(1)).revoke("jti-999");
    }

    @Test
    void logout_doesNotThrow_whenTokenAlreadyInvalid() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("bad-token");

        when(jwtUtil.extractTokenId("bad-token")).thenThrow(new RuntimeException("malformed"));

        authService.logout(request); // should swallow the exception, not propagate

        verify(refreshTokenService, never()).revoke(anyString());
    }
}
