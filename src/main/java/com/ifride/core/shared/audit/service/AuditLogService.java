package com.ifride.core.shared.audit.service;

import com.ifride.core.shared.audit.AuditLog;
import com.ifride.core.shared.audit.enums.AuditAction;
import com.ifride.core.shared.audit.enums.TargetType;
import com.ifride.core.shared.audit.repository.AuditLogRepository;
import com.ifride.core.shared.services.DeterministicEncryptorService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;
    private final DeterministicEncryptorService crypto;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(AuditAction action, String targetId, TargetType targetType, String authorId, String ipAddress) {
        var encryptedIp = StringUtils.isEmpty(ipAddress) ? null : crypto.encrypt(ipAddress);
        AuditLog log = AuditLog.builder()
                .action(action)
                .targetId(targetId)
                .targetType(targetType)
                .authorId(authorId)
                .ipAddressEncrypted(encryptedIp)
                .build();

        repository.save(log);
    }
}