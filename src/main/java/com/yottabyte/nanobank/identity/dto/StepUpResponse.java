package com.yottabyte.nanobank.identity.dto;

import com.yottabyte.nanobank.identity.enums.OnboardingStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStep;

import java.util.UUID;

public record StepUpResponse(
        UUID applicationId,
        OnboardingStatus status,
        OnboardingStep currentStep,
        String message
) {
}
