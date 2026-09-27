package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.ManualReviewRequest;
import com.yottabyte.nanobank.identity.dto.ManualReviewResponse;
import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.entity.ManualReview;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.AuditEventType;
import com.yottabyte.nanobank.identity.enums.CustomerStatus;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.enums.ManualReviewStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStep;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.exception.OnboardingNotFoundException;
import com.yottabyte.nanobank.identity.repository.ManualReviewRepository;
import com.yottabyte.nanobank.identity.repository.OnboardingApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualReviewServiceTest {

    @Mock
    private ManualReviewRepository manualReviewRepository;

    @Mock
    private OnboardingApplicationRepository onboardingRepository;

    @Mock
    private KycService kycService;

    @Mock
    private CustomerService customerService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ManualReviewService manualReviewService;

    @Test
    void shouldCreateManualReview() {

        OnboardingApplication application =
                createManualReviewApplication();

        when(manualReviewRepository
                .findByOnboardingApplicationId(application.getId()))
                .thenReturn(Optional.empty());

        manualReviewService.createReview(
                application,
                "Fraud screening requires manual review",
                "CORR-001"
        );

        ArgumentCaptor<ManualReview> captor =
                ArgumentCaptor.forClass(ManualReview.class);

        verify(manualReviewRepository)
                .save(captor.capture());

        ManualReview review = captor.getValue();

        assertEquals(
                application,
                review.getOnboardingApplication()
        );

        assertEquals(
                ManualReviewStatus.WAITING_REVIEW,
                review.getStatus()
        );

        assertEquals(
                "Fraud screening requires manual review",
                review.getReason()
        );

        assertNotNull(review.getCreatedAt());

        assertEquals(
                OnboardingStatus.MANUAL_REVIEW,
                application.getStatus()
        );

        assertEquals(
                OnboardingStep.MANUAL_REVIEW,
                application.getCurrentStep()
        );

        verify(onboardingRepository)
                .save(application);

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.MANUAL_REVIEW_CREATED),
                        eq("CORR-001"),
                        eq("Fraud screening requires manual review")
                );
    }

    @Test
    void shouldNotCreateDuplicateWaitingReview() {

        OnboardingApplication application =
                createManualReviewApplication();

        ManualReview existingReview =
                new ManualReview();

        existingReview.setStatus(
                ManualReviewStatus.WAITING_REVIEW
        );

        when(manualReviewRepository
                .findByOnboardingApplicationId(application.getId()))
                .thenReturn(Optional.of(existingReview));

        manualReviewService.createReview(
                application,
                "Another reason",
                "CORR-002"
        );

        verify(manualReviewRepository, never())
                .save(any(ManualReview.class));

        verify(onboardingRepository, never())
                .save(any(OnboardingApplication.class));

        verify(auditService, never())
                .record(any(), any(), any(), any(), any());
    }

    @Test
    void shouldApproveManualReviewAndCreateCustomer() {

        OnboardingApplication application =
                createManualReviewApplication();

        ManualReview review =
                createWaitingReview(application);

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-123",
                        "PASS",
                        "Documents verified manually"
                );

        Customer customer =
                Customer.builder()
                        .id(UUID.randomUUID())
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .status(CustomerStatus.ACTIVE)
                        .build();

        when(onboardingRepository.findById(application.getId()))
                .thenReturn(Optional.of(application));

        when(manualReviewRepository
                .findByOnboardingApplicationId(application.getId()))
                .thenReturn(Optional.of(review));

        when(customerService.createFromOnboarding(application))
                .thenAnswer(invocation -> {
                    application.setCustomerId(customer.getId());
                    return customer;
                });

        ManualReviewResponse response =
                manualReviewService.review(
                        application.getId(),
                        request,
                        "CORR-003"
                );

        assertNotNull(response);

        assertEquals(
                ManualReviewStatus.APPROVED,
                response.reviewStatus()
        );

        assertEquals(
                OnboardingStatus.CUSTOMER_CREATED,
                response.onboardingStatus()
        );

        assertEquals(
                customer.getId(),
                response.customerId()
        );

        assertEquals(
                ManualReviewStatus.APPROVED,
                review.getStatus()
        );

        assertEquals(
                "reviewer-123",
                review.getReviewerId()
        );

        assertEquals(
                "Documents verified manually",
                review.getReviewerComment()
        );

        assertNotNull(review.getCompletedAt());

        assertEquals(
                OnboardingStatus.CUSTOMER_CREATED,
                application.getStatus()
        );

        assertEquals(
                OnboardingStep.COMPLETED,
                application.getCurrentStep()
        );

        verify(manualReviewRepository)
                .save(review);

        verify(kycService)
                .changeStatus(
                        application,
                        KycStatus.ACCEPTED,
                        "Manual review approved"
                );

        verify(customerService)
                .createFromOnboarding(application);

        verify(onboardingRepository, times(2))
                .save(application);

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.MANUAL_REVIEW_COMPLETED),
                        eq("CORR-003"),
                        eq("Manual review approved")
                );
    }

    @Test
    void shouldRejectManualReview() {

        OnboardingApplication application =
                createManualReviewApplication();

        ManualReview review =
                createWaitingReview(application);

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-456",
                        "FAIL",
                        "Identity documents could not be verified"
                );

        when(onboardingRepository.findById(application.getId()))
                .thenReturn(Optional.of(application));

        when(manualReviewRepository
                .findByOnboardingApplicationId(application.getId()))
                .thenReturn(Optional.of(review));

        ManualReviewResponse response =
                manualReviewService.review(
                        application.getId(),
                        request,
                        "CORR-004"
                );

        assertNotNull(response);

        assertEquals(
                ManualReviewStatus.REJECTED,
                response.reviewStatus()
        );

        assertEquals(
                OnboardingStatus.REJECTED,
                response.onboardingStatus()
        );

        assertNull(response.customerId());

        assertEquals(
                ManualReviewStatus.REJECTED,
                review.getStatus()
        );

        assertEquals(
                "reviewer-456",
                review.getReviewerId()
        );

        assertEquals(
                "Identity documents could not be verified",
                review.getReviewerComment()
        );

        assertNotNull(review.getCompletedAt());

        assertEquals(
                OnboardingStatus.REJECTED,
                application.getStatus()
        );

        assertEquals(
                OnboardingStep.COMPLETED,
                application.getCurrentStep()
        );

        verify(manualReviewRepository)
                .save(review);

        verify(kycService)
                .changeStatus(
                        application,
                        KycStatus.REJECTED,
                        "Manual review rejected"
                );

        verify(customerService, never())
                .createFromOnboarding(any());

        verify(onboardingRepository)
                .save(application);

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.MANUAL_REVIEW_COMPLETED),
                        eq("CORR-004"),
                        eq("Manual review rejected")
                );

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.ONBOARDING_REJECTED),
                        eq("CORR-004"),
                        eq("Manual review rejected")
                );
    }

    @Test
    void shouldThrowWhenApplicationDoesNotExist() {

        UUID applicationId = UUID.randomUUID();

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-123",
                        "PASS",
                        "Approved"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(Optional.empty());

        assertThrows(
                OnboardingNotFoundException.class,
                () -> manualReviewService.review(
                        applicationId,
                        request,
                        "CORR-005"
                )
        );

        verify(manualReviewRepository, never())
                .findByOnboardingApplicationId(any());
    }

    @Test
    void shouldThrowWhenApplicationIsNotInManualReviewState() {

        OnboardingApplication application =
                createManualReviewApplication();

        application.setStatus(
                OnboardingStatus.APPROVED
        );

        UUID applicationId = application.getId();

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-123",
                        "PASS",
                        "Approved"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> manualReviewService.review(
                                applicationId,
                                request,
                                "CORR-006"
                        )
                );

        assertEquals(
                "Application is not waiting for manual review",
                exception.getMessage()
        );

        verify(manualReviewRepository, never())
                .findByOnboardingApplicationId(any());
    }

    @Test
    void shouldThrowWhenManualReviewRecordDoesNotExist() {

        OnboardingApplication application =
                createManualReviewApplication();

        UUID applicationId = application.getId();

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-123",
                        "PASS",
                        "Approved"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(manualReviewRepository
                .findByOnboardingApplicationId(applicationId))
                .thenReturn(Optional.empty());

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> manualReviewService.review(
                                applicationId,
                                request,
                                "CORR-007"
                        )
                );

        assertEquals(
                "Manual review record does not exist",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenManualReviewAlreadyCompleted() {

        OnboardingApplication application =
                createManualReviewApplication();

        ManualReview review =
                createWaitingReview(application);

        review.setStatus(
                ManualReviewStatus.APPROVED
        );

        UUID applicationId = application.getId();

        ManualReviewRequest request =
                new ManualReviewRequest(
                        "reviewer-123",
                        "PASS",
                        "Approved"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(manualReviewRepository
                .findByOnboardingApplicationId(applicationId))
                .thenReturn(Optional.of(review));

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> manualReviewService.review(
                                applicationId,
                                request,
                                "CORR-008"
                        )
                );

        assertEquals(
                "Manual review has already been completed",
                exception.getMessage()
        );
    }

    private OnboardingApplication createManualReviewApplication() {

        return OnboardingApplication.builder()
                .id(UUID.randomUUID())
                .applicationReference("NB-REVIEW123")
                .status(OnboardingStatus.MANUAL_REVIEW)
                .currentStep(OnboardingStep.MANUAL_REVIEW)
                .firstName("Wayne")
                .lastName("Mokoena")
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9805105000088")
                .kycStatus(KycStatus.PENDING)
                .build();
    }

    private ManualReview createWaitingReview(
            OnboardingApplication application
    ) {

        ManualReview review =
                new ManualReview();

        review.setOnboardingApplication(application);
        review.setStatus(
                ManualReviewStatus.WAITING_REVIEW
        );
        review.setReason(
                "Manual verification required"
        );

        return review;
    }
}