package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingApplicationRepository
        extends JpaRepository<OnboardingApplication, UUID> {

    Optional<OnboardingApplication> findByApplicationReference(
            String applicationReference
    );

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNationalId(String nationalId);
}
