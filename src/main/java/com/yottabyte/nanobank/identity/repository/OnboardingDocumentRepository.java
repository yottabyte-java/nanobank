package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingDocumentRepository
        extends JpaRepository<OnboardingDocument, UUID> {

    List<OnboardingDocument> findByOnboardingApplicationId(
            UUID onboardingApplicationId
    );
}
