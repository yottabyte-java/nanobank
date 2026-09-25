package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, UUID> {
}
