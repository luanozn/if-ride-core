package com.ifride.core.shared.audit.service;

import com.ifride.core.shared.audit.AuditLog;
import com.ifride.core.shared.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String targetId, String targetType, String authorId, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID().toString())
                .action(action)
                .targetId(targetId)
                .targetType(targetType)
                .authorId(authorId)
                .ipAddress(ipAddress)
                .build();

        repository.save(log);
    }
}