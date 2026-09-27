package com.yottabyte.nanobank.identity.provider.mock;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.provider.IdentityVerificationProvider;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import org.springframework.stereotype.Component;

@Component
public class MockIdentityVerificationProvider
        implements IdentityVerificationProvider {

    @Override
    public VerificationResult verify(OnboardingApplication application) {

        /*
         * Local development
         *
         *
         * Later this adapter can call a real identity provider.
         */
        return VerificationResult.pass(
                "MOCK_IDENTITY_PROVIDER",
                "identity-" + application.getId()
        );
    }
}
