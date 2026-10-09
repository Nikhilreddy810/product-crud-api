package com.nikhil.productapi.service.impl;

import com.nikhil.productapi.entity.AuditLog;
import com.nikhil.productapi.repository.AuditLogRepository;
import com.nikhil.productapi.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Writes an audit trail row for every product create/update/delete.
 * Runs on the "taskExecutor" thread pool (see AsyncConfig) so it never
 * blocks or slows down the API response to the caller.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async("taskExecutor")
    public void logProductChange(Integer productId, String action, String performedBy) {
        try {
            AuditLog entry = AuditLog.builder()
                    .productId(productId)
                    .action(action)
                    .performedBy(performedBy)
                    .performedOn(LocalDateTime.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // audit failures must never break the main request flow (fire-and-forget)
            log.warn("Failed to write audit log for product {} action {}: {}", productId, action, e.getMessage());
        }
    }
}
