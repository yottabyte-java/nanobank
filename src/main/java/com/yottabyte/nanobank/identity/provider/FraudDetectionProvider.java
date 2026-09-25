package com.yottabyte.nanobank.identity.provider;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;

public interface FraudDetectionProvider {

    VerificationResult screen(OnboardingApplication application);
}
