package com.yottabyte.nanobank.identity.provider.mock;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.provider.FraudDetectionProvider;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import org.springframework.stereotype.Component;

@Component
public class MockFraudDetectionProvider
        implements FraudDetectionProvider {

    @Override
    public VerificationResult screen(OnboardingApplication application) {

        return VerificationResult.pass(
                "MOCK_FRAUD_PROVIDER",
                "fraud-" + application.getId()
        );
    }
}
