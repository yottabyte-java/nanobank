package com.yottabyte.nanobank.identity.provider;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;

public interface DocumentVerificationProvider {

    VerificationResult verify(OnboardingApplication application);
}
