package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.KycHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KycHistoryRepository
        extends JpaRepository<KycHistory, UUID> {
}
