package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.ManualReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ManualReviewRepository
        extends JpaRepository<ManualReview, UUID> {

    Optional<ManualReview> findByOnboardingApplicationId(UUID applicationId);
}
