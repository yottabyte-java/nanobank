package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.StepUpRequest;
import com.yottabyte.nanobank.identity.dto.StepUpResponse;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.entity.OnboardingDocument;
import com.yottabyte.nanobank.identity.entity.VerificationCheck;
import com.yottabyte.nanobank.identity.enums.*;
import com.yottabyte.nanobank.identity.enums.*;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.exception.OnboardingNotFoundException;
import com.yottabyte.nanobank.identity.repository.OnboardingApplicationRepository;
import com.yottabyte.nanobank.identity.repository.OnboardingDocumentRepository;
import com.yottabyte.nanobank.identity.repository.VerificationCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StepUpService {

    private final OnboardingApplicationRepository onboardingRepository;
    private final OnboardingDocumentRepository documentRepository;
    private final VerificationCheckRepository verificationCheckRepository;

    private final ManualReviewService manualReviewService;
    private final KycService kycService;
    private final CustomerService customerService;
    private final AuditService auditService;

    @Transactional
    public StepUpResponse submit(
            UUID applicationId,
            StepUpRequest request,
            String correlationId
    ) {

        OnboardingApplication application =
                onboardingRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new OnboardingNotFoundException(applicationId.toString()));

        validateState(application);

        OnboardingDocument document = new OnboardingDocument();

        document.setOnboardingApplication(application);
        IdentityDocumentType documentType;

        try {
            documentType = IdentityDocumentType.valueOf(
                    request.documentType().trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidOnboardingStateException(
                    "Invalid document type: " + request.documentType()
            );
        }

        document.setDocumentType(documentType);
        document.setDocumentNumber(request.documentNumber());
        document.setStorageReference(request.storageReference());
        document.setVerificationStatus(VerificationStatus.IN_PROGRESS);

        documentRepository.save(document);

        application.setStatus(
                OnboardingStatus.VERIFICATION_IN_PROGRESS
        );

        application.setCurrentStep(
                OnboardingStep.STEP_UP
        );

        onboardingRepository.save(application);

        VerificationCheck check = performVerification(application);

        verificationCheckRepository.save(check);

        if (check.getDecision() == Decision.FAIL) {
            return reject(
                    application,
                    "Step-up verification failed",
                    correlationId
            );
        }

        if (check.getDecision() == Decision.REFER) {

            application.setStatus(
                    OnboardingStatus.MANUAL_REVIEW
            );

            application.setCurrentStep(
                    OnboardingStep.MANUAL_REVIEW
            );

            onboardingRepository.save(application);

            manualReviewService.createReview(
                    application,
                    "Step-up verification requires manual review",
                    correlationId
            );

            return new StepUpResponse(
                    application.getId(),
                    application.getStatus(),
                    application.getCurrentStep(),
                    "Application sent for manual review"
            );
        }

        /*
         * STEP-UP PASSED
         */
        kycService.changeStatus(
                application,
                KycStatus.ACCEPTED,
                "Step-up verification passed"
        );

        application.setStatus(
                OnboardingStatus.APPROVED
        );

        application.setCurrentStep(
                OnboardingStep.COMPLETED
        );

        onboardingRepository.save(application);

        customerService.createFromOnboarding(application);

        application.setStatus(
                OnboardingStatus.CUSTOMER_CREATED
        );

        onboardingRepository.save(application);

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.ONBOARDING_APPROVED,
                correlationId,
                "Step-up verification passed"
        );

        return new StepUpResponse(
                application.getId(),
                application.getStatus(),
                application.getCurrentStep(),
                "Step-up verification passed and customer created"
        );
    }

    private void validateState(
            OnboardingApplication application
    ) {

        if (application.getStatus()
                != OnboardingStatus.STEP_UP_REQUIRED) {

            throw new InvalidOnboardingStateException(
                    "Application is not waiting for step-up verification"
            );
        }

        if (application.getCurrentStep()
                != OnboardingStep.STEP_UP) {

            throw new InvalidOnboardingStateException(
                    "Application is not currently at STEP_UP"
            );
        }
    }

    private VerificationCheck performVerification(
            OnboardingApplication application
    ) {

        /*
         * Temporary mock.
         *
         * PASS = normal flow
         *
         * Change to REFER when testing manual review.
         */
        return VerificationCheck.builder()
                .onboardingApplication(application)
                .verificationType(VerificationType.DOCUMENT)
                .status(VerificationStatus.COMPLETED)
                .decision(Decision.PASS)
                .provider("MOCK_STEP_UP_PROVIDER")
                .providerReference(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .build();
    }

    private StepUpResponse reject(
            OnboardingApplication application,
            String reason,
            String correlationId
    ) {

        application.setStatus(
                OnboardingStatus.REJECTED
        );

        application.setCurrentStep(
                OnboardingStep.COMPLETED
        );

        kycService.changeStatus(
                application,
                KycStatus.REJECTED,
                reason
        );

        onboardingRepository.save(application);

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.ONBOARDING_REJECTED,
                correlationId,
                reason
        );

        return new StepUpResponse(
                application.getId(),
                application.getStatus(),
                application.getCurrentStep(),
                reason
        );
    }
}
