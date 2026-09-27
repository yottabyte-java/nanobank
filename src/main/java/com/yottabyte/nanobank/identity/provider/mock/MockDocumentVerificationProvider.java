package com.yottabyte.nanobank.identity.provider.mock;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.provider.DocumentVerificationProvider;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import org.springframework.stereotype.Component;

@Component
public class MockDocumentVerificationProvider
        implements DocumentVerificationProvider {

    @Override
    public VerificationResult verify(OnboardingApplication application) {

        return VerificationResult.pass(
                "MOCK_DOCUMENT_PROVIDER",
                "document-" + application.getId()
        );
    }
}
