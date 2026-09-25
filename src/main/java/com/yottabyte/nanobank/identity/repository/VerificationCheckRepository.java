package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.VerificationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VerificationCheckRepository
        extends JpaRepository<VerificationCheck, UUID> {

    List<VerificationCheck> findByOnboardingApplicationId(UUID applicationId);
}
