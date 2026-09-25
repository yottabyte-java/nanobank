package com.yottabyte.nanobank.identity.service;




import com.yottabyte.nanobank.identity.dto.ManualReviewRequest;
import com.yottabyte.nanobank.identity.dto.ManualReviewResponse;
import com.yottabyte.nanobank.identity.entity.ManualReview;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.AuditEventType;
import com.yottabyte.nanobank.identity.enums.Decision;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.enums.ManualReviewStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStep;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.exception.OnboardingNotFoundException;
import com.yottabyte.nanobank.identity.repository.ManualReviewRepository;
import com.yottabyte.nanobank.identity.repository.OnboardingApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManualReviewService {

    private final ManualReviewRepository manualReviewRepository;
    private final OnboardingApplicationRepository onboardingRepository;

    private final KycService kycService;
    private final CustomerService customerService;
    private final AuditService auditService;

    @Transactional
    public void createReview(
            OnboardingApplication application,
            String reason,
            String correlationId
    ) {

        var existing =
                manualReviewRepository.findByOnboardingApplicationId(
                        application.getId()
                );

        if (existing.isPresent()
                && existing.get().getStatus()
                == ManualReviewStatus.WAITING_REVIEW) {

            return;
        }

        ManualReview review = new ManualReview();

        review.setOnboardingApplication(application);
        review.setStatus(ManualReviewStatus.WAITING_REVIEW);
        review.setReason(reason);
        review.setCreatedAt(Instant.now());

        manualReviewRepository.save(review);

        application.setStatus(
                OnboardingStatus.MANUAL_REVIEW
        );

        application.setCurrentStep(
                OnboardingStep.MANUAL_REVIEW
        );

        onboardingRepository.save(application);

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.MANUAL_REVIEW_CREATED,
                correlationId,
                reason
        );
    }

    @Transactional
    public ManualReviewResponse review(
            UUID applicationId,
            ManualReviewRequest request,
            String correlationId
    ) {

        OnboardingApplication application =
                onboardingRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new OnboardingNotFoundException(
                                        applicationId.toString()
                                ));

        validateState(application);

        ManualReview review =
                manualReviewRepository
                        .findByOnboardingApplicationId(applicationId)
                        .orElseThrow(() ->
                                new InvalidOnboardingStateException(
                                        "Manual review record does not exist"
                                ));

        if (review.getStatus()
                != ManualReviewStatus.WAITING_REVIEW) {

            throw new InvalidOnboardingStateException(
                    "Manual review has already been completed"
            );
        }

        Decision decision;

        try {
            decision = Decision.valueOf(
                    request.decision()
                            .trim()
                            .toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidOnboardingStateException(
                    "Manual review decision must be PASS or FAIL"
            );
        }

        if (decision == Decision.PASS) {

            return approve(
                    application,
                    review,
                    request,
                    correlationId
            );
        }

        if (decision == Decision.FAIL) {

            return reject(
                    application,
                    review,
                    request,
                    correlationId
            );
        }

        throw new InvalidOnboardingStateException(
                "Manual review decision must be PASS or FAIL"
        );
    }

    private ManualReviewResponse approve(
            OnboardingApplication application,
            ManualReview review,
            ManualReviewRequest request,
            String correlationId
    ) {

        review.setStatus(
                ManualReviewStatus.APPROVED
        );

        review.setReviewerId(
                request.reviewerId()
        );

        review.setReviewerComment(
                request.comment()
        );

        review.setCompletedAt(
                Instant.now()
        );

        manualReviewRepository.save(review);

        kycService.changeStatus(
                application,
                KycStatus.ACCEPTED,
                "Manual review approved"
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
                AuditEventType.MANUAL_REVIEW_COMPLETED,
                correlationId,
                "Manual review approved"
        );

        return new ManualReviewResponse(
                application.getId(),
                review.getStatus(),
                application.getStatus(),
                application.getCustomerId(),
                "Manual review approved and customer created"
        );
    }

    private ManualReviewResponse reject(
            OnboardingApplication application,
            ManualReview review,
            ManualReviewRequest request,
            String correlationId
    ) {

        review.setStatus(
                ManualReviewStatus.REJECTED
        );

        review.setReviewerId(
                request.reviewerId()
        );

        review.setReviewerComment(
                request.comment()
        );

        review.setCompletedAt(
                Instant.now()
        );

        manualReviewRepository.save(review);

        kycService.changeStatus(
                application,
                KycStatus.REJECTED,
                "Manual review rejected"
        );

        application.setStatus(
                OnboardingStatus.REJECTED
        );

        application.setCurrentStep(
                OnboardingStep.COMPLETED
        );

        onboardingRepository.save(application);

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.MANUAL_REVIEW_COMPLETED,
                correlationId,
                "Manual review rejected"
        );

        auditService.record(
                "ONBOARDING_APPLICATION",
                application.getId(),
                AuditEventType.ONBOARDING_REJECTED,
                correlationId,
                "Manual review rejected"
        );

        return new ManualReviewResponse(
                application.getId(),
                review.getStatus(),
                application.getStatus(),
                null,
                "Manual review rejected"
        );
    }

    private void validateState(
            OnboardingApplication application
    ) {

        if (application.getStatus()
                != OnboardingStatus.MANUAL_REVIEW) {

            throw new InvalidOnboardingStateException(
                    "Application is not waiting for manual review"
            );
        }

        if (application.getCurrentStep()
                != OnboardingStep.MANUAL_REVIEW) {

            throw new InvalidOnboardingStateException(
                    "Application is not currently at MANUAL_REVIEW"
            );
        }
    }
}

