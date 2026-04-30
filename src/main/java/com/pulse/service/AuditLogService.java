package com.pulse.service;

import com.pulse.entity.AuditLog;
import com.pulse.entity.AuditLogEventType;
import com.pulse.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogEventType eventType,
                       Long actorUserId,
                       String actorIdentifier,
                       String targetType,
                       Long targetId,
                       String targetName,
                       String details) {
        AuditLog auditLog = AuditLog.builder()
                .eventType(eventType)
                .actorUserId(actorUserId)
                .actorIdentifier(actorIdentifier)
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }
}