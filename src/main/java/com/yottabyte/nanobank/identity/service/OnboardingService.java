package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.OnboardingResponse;
import com.yottabyte.nanobank.identity.dto.StartOnboardingRequest;
import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.*;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.exception.OnboardingNotFoundException;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import com.yottabyte.nanobank.identity.repository.OnboardingApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final IdempotencyService idempotencyService;
    private final AddressService addressService;
    private final OnboardingApplicationRepository applicationRepository;
    private final VerificationOrchestrator verificationOrchestrator;
    private final CustomerService customerService;
    private final KycService kycService;
    private final AuditService auditService;

    @Transactional
    public OnboardingResponse start(
            StartOnboardingRequest request,
            String idempotencyKey,
            String correlationId
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidOnboardingStateException(
                    "Idempotency-Key is required."
            );
        }

        String requestHash =
                idempotencyService.createRequestHash(request);

        OnboardingResponse existingResponse =
                idempotencyService.findExisting(
                        idempotencyKey,
                        requestHash
                );

        if (existingResponse != null) {
            return existingResponse;
        }

        validateDuplicateIdentity(request);

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .applicationReference(generateReference())
                        .status(OnboardingStatus.STARTED)
                        .currentStep(OnboardingStep.CUSTOMER_INFORMATION)
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .dateOfBirth(request.dateOfBirth())
                        .email(request.email().toLowerCase())
                        .mobileNumber(request.mobileNumber())
                        .nationalId(request.nationalId())
                        .kycStatus(KycStatus.PENDING)
                        .build();

        application = applicationRepository.save(application);

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.ONBOARDING_STARTED,
                correlationId,
                "Onboarding application created."
        );

        application.setStatus(
                OnboardingStatus.VERIFICATION_IN_PROGRESS
        );

        application.setCurrentStep(
                OnboardingStep.IDENTITY_VERIFICATION
        );

        application = applicationRepository.save(application);

        List<VerificationResult> results =
                verificationOrchestrator.verify(application);

        Decision overallDecision = evaluate(results);

        if (overallDecision == Decision.FAIL) {

            application.setStatus(
                    OnboardingStatus.REJECTED
            );

            application.setCurrentStep(
                    OnboardingStep.COMPLETED
            );

            kycService.changeStatus(
                    application,
                    KycStatus.REJECTED,
                    "Automated verification failed."
            );

            auditService.record(
                    "ONBOARDING_APPLICATION",
                    application.getId(),
                    AuditEventType.ONBOARDING_REJECTED,
                    correlationId,
                    "Automated verification failed."
            );

        } else if (overallDecision == Decision.REFER) {

            application.setStatus(
                    OnboardingStatus.STEP_UP_REQUIRED
            );

            application.setCurrentStep(
                    OnboardingStep.STEP_UP
            );

            auditService.record(
                    "ONBOARDING_APPLICATION",
                    application.getId(),
                    AuditEventType.STEP_UP_REQUESTED,
                    correlationId,
                    "Additional verification required."
            );

        } else {

            approveApplication(
                    application,
                    request,
                    correlationId
            );
        }

        applicationRepository.save(application);

        OnboardingResponse response =
                toResponse(application);

        idempotencyService.saveResponse(
                idempotencyKey,
                "START_ONBOARDING",
                requestHash,
                response
        );

        return response;
    }

    private void approveApplication(
            OnboardingApplication application,
            StartOnboardingRequest request,
            String correlationId
    ) {

        application.setStatus(
                OnboardingStatus.APPROVED
        );

        application.setCurrentStep(
                OnboardingStep.COMPLETED
        );

        kycService.changeStatus(
                application,
                KycStatus.ACCEPTED,
                "All automated checks passed."
        );

        Customer customer =
                customerService.createFromOnboarding(
                        application
                );

        addressService.createForCustomer(
                customer,
                request
        );

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.ONBOARDING_APPROVED,
                correlationId,
                "Customer onboarding approved."
        );

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.CUSTOMER_CREATED,
                correlationId,
                "Customer created."
        );
    }

    private Decision evaluate(
            List<VerificationResult> results
    ) {

        boolean hasFailure =
                results.stream()
                        .anyMatch(
                                result ->
                                        result.decision()
                                                == Decision.FAIL
                        );

        if (hasFailure) {
            return Decision.FAIL;
        }

        boolean needsReview =
                results.stream()
                        .anyMatch(
                                result ->
                                        result.decision()
                                                == Decision.REFER
                        );

        if (needsReview) {
            return Decision.REFER;
        }

        return Decision.PASS;
    }

    private void validateDuplicateIdentity(
            StartOnboardingRequest request
    ) {

        if (applicationRepository.existsByEmailIgnoreCase(
                request.email()
        )
                || applicationRepository.existsByNationalId(
                request.nationalId()
        )) {

            throw new InvalidOnboardingStateException(
                    "An onboarding application already exists for this identity."
            );
        }
    }

    private String generateReference() {

        return "NB-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();
    }

    @Transactional(readOnly = true)
    public OnboardingResponse get(
            UUID applicationId
    ) {

        OnboardingApplication application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new OnboardingNotFoundException(
                                        "Onboarding application not found."
                                )
                        );

        return toResponse(application);
    }

    private OnboardingResponse toResponse(
            OnboardingApplication application
    ) {

        return new OnboardingResponse(
                application.getId(),
                application.getApplicationReference(),
                application.getStatus(),
                application.getCurrentStep(),
                application.getKycStatus(),
                application.getCustomerId(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}