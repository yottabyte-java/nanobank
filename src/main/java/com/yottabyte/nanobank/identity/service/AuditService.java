package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.AuditEvent;
import com.yottabyte.nanobank.identity.enums.AuditEventType;
import com.yottabyte.nanobank.identity.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void record(
            String entityType,
            UUID entityId,
            AuditEventType eventType,
            String correlationId,
            String metadata
    ) {

        AuditEvent event = AuditEvent.builder()
                .entityType(entityType)
                .entityId(entityId)
                .eventType(eventType)
                .actorType("SYSTEM")
                .correlationId(correlationId)
                .metadata(metadata)
                .build();

        auditEventRepository.save(event);
    }
}
