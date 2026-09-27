package com.yottabyte.nanobank.identity.dto;

import com.yottabyte.nanobank.identity.enums.ManualReviewStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStatus;

import java.util.UUID;

public record ManualReviewResponse(
        UUID applicationId,
        ManualReviewStatus reviewStatus,
        OnboardingStatus onboardingStatus,
        UUID customerId,
        String message
) {
}
