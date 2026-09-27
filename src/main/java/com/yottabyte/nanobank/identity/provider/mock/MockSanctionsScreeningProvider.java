package com.yottabyte.nanobank.identity.provider.mock;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.provider.SanctionsScreeningProvider;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import org.springframework.stereotype.Component;

@Component
public class MockSanctionsScreeningProvider
        implements SanctionsScreeningProvider {

    @Override
    public VerificationResult screen(OnboardingApplication application) {

        return VerificationResult.pass(
                "MOCK_SANCTIONS_PROVIDER",
                "sanctions-" + application.getId()
        );
    }
}
