package com.yottabyte.nanobank.identity.dto;

import com.yottabyte.nanobank.identity.enums.*;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStep;

import java.time.Instant;
import java.util.UUID;

public record OnboardingResponse(

        UUID applicationId,

        String applicationReference,

        OnboardingStatus status,

        OnboardingStep currentStep,

        KycStatus kycStatus,

        UUID customerId,

        Instant createdAt,

        Instant updatedAt
) {
}
