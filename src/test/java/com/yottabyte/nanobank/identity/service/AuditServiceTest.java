package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.AuditEvent;
import com.yottabyte.nanobank.identity.enums.AuditEventType;
import com.yottabyte.nanobank.identity.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void shouldCreateAndSaveAuditEvent() {

        UUID entityId = UUID.randomUUID();

        auditService.record(
                "ONBOARDING_APPLICATION",
                entityId,
                AuditEventType.ONBOARDING_STARTED,
                "CORR-001",
                "Onboarding application created."
        );

        ArgumentCaptor<AuditEvent> captor =
                ArgumentCaptor.forClass(AuditEvent.class);

        verify(auditEventRepository)
                .save(captor.capture());

        AuditEvent event = captor.getValue();

        assertEquals(
                "ONBOARDING_APPLICATION",
                event.getEntityType()
        );

        assertEquals(
                entityId,
                event.getEntityId()
        );

        assertEquals(
                AuditEventType.ONBOARDING_STARTED,
                event.getEventType()
        );

        assertEquals(
                "SYSTEM",
                event.getActorType()
        );

        assertEquals(
                "CORR-001",
                event.getCorrelationId()
        );

        assertEquals(
                "Onboarding application created.",
                event.getMetadata()
        );
    }
}