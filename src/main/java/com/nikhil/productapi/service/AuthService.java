package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.AuthRequest;
import com.nikhil.productapi.dto.AuthResponse;
import com.nikhil.productapi.dto.RefreshRequest;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    AuthResponse refresh(RefreshRequest request);
    void logout(RefreshRequest request);
}
