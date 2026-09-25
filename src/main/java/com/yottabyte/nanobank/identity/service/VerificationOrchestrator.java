package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.entity.VerificationCheck;
import com.yottabyte.nanobank.identity.enums.Decision;
import com.yottabyte.nanobank.identity.enums.VerificationStatus;
import com.yottabyte.nanobank.identity.enums.VerificationType;
import com.yottabyte.nanobank.identity.provider.*;
import com.yottabyte.nanobank.identity.repository.VerificationCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationOrchestrator {

    private final IdentityVerificationProvider identityProvider;
    private final DocumentVerificationProvider documentProvider;
    private final FraudDetectionProvider fraudProvider;
    private final SanctionsScreeningProvider sanctionsProvider;

    private final VerificationCheckRepository verificationRepository;

    @Transactional
    public List<VerificationResult> verify(
            OnboardingApplication application
    ) {

        List<VerificationResult> results = new ArrayList<>();

        results.add(
                execute(
                        application,
                        VerificationType.IDENTITY,
                        identityProvider.verify(application)
                )
        );

        results.add(
                execute(
                        application,
                        VerificationType.DOCUMENT,
                        documentProvider.verify(application)
                )
        );

        results.add(
                execute(
                        application,
                        VerificationType.FRAUD,
                        fraudProvider.screen(application)
                )
        );

        results.add(
                execute(
                        application,
                        VerificationType.SANCTIONS,
                        sanctionsProvider.screen(application)
                )
        );

        return results;
    }

    private VerificationResult execute(
            OnboardingApplication application,
            VerificationType type,
            VerificationResult result
    ) {

        VerificationStatus status =
                result.decision() == Decision.FAIL
                        ? VerificationStatus.FAILED
                        : VerificationStatus.COMPLETED;

        VerificationCheck check = VerificationCheck.builder()
                .onboardingApplication(application)
                .verificationType(type)
                .status(status)
                .decision(result.decision())
                .provider(result.provider())
                .providerReference(result.providerReference())
                .failureReason(result.reason())
                .completedAt(Instant.now())
                .build();

        verificationRepository.save(check);

        return result;
    }
}
