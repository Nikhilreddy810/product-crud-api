package com.nikhil.productapi.service;

public interface AuditLogService {
    void logProductChange(Integer productId, String action, String performedBy);
}
