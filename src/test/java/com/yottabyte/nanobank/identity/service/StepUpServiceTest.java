package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.StepUpRequest;
import com.yottabyte.nanobank.identity.dto.StepUpResponse;
import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.entity.OnboardingDocument;
import com.yottabyte.nanobank.identity.entity.VerificationCheck;
import com.yottabyte.nanobank.identity.enums.*;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.exception.OnboardingNotFoundException;
import com.yottabyte.nanobank.identity.repository.OnboardingApplicationRepository;
import com.yottabyte.nanobank.identity.repository.OnboardingDocumentRepository;
import com.yottabyte.nanobank.identity.repository.VerificationCheckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StepUpServiceTest {

    @Mock
    private OnboardingApplicationRepository onboardingRepository;

    @Mock
    private OnboardingDocumentRepository documentRepository;

    @Mock
    private VerificationCheckRepository verificationCheckRepository;

    @Mock
    private ManualReviewService manualReviewService;

    @Mock
    private KycService kycService;

    @Mock
    private CustomerService customerService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private StepUpService stepUpService;

    @Test
    void shouldCompleteStepUpAndCreateCustomerWhenVerificationPasses() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createStepUpApplication(applicationId);

        StepUpRequest request =
                new StepUpRequest(
                        "SOUTH_AFRICAN_ID",
                        "9001015000088",
                        "s3://nanobank/test-document"
                );

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Wayne")
                .lastName("Mokoena")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9001015000088")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.of(application));

        when(customerService.createFromOnboarding(application))
                .thenReturn(customer);

        StepUpResponse response =
                stepUpService.submit(
                        applicationId,
                        request,
                        "CORR-STEP-UP-001"
                );

        assertEquals(
                applicationId,
                response.applicationId()
        );

        assertEquals(
                OnboardingStatus.CUSTOMER_CREATED,
                response.status()
        );

        assertEquals(
                OnboardingStep.COMPLETED,
                response.currentStep()
        );

        assertEquals(
                "Step-up verification passed and customer created",
                response.message()
        );

        verify(documentRepository)
                .save(any(OnboardingDocument.class));

        verify(verificationCheckRepository)
                .save(any(VerificationCheck.class));

        verify(kycService)
                .changeStatus(
                        application,
                        KycStatus.ACCEPTED,
                        "Step-up verification passed"
                );

        verify(customerService)
                .createFromOnboarding(application);

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(applicationId),
                        eq(AuditEventType.ONBOARDING_APPROVED),
                        eq("CORR-STEP-UP-001"),
                        eq("Step-up verification passed")
                );
    }

    @Test
    void shouldThrowExceptionWhenApplicationDoesNotExist() {

        UUID applicationId = UUID.randomUUID();

        StepUpRequest request =
                new StepUpRequest(
                        "SOUTH_AFRICAN_ID",
                        "9001015000088",
                        "s3://nanobank/test-document"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                OnboardingNotFoundException.class,
                () -> stepUpService.submit(
                        applicationId,
                        request,
                        "CORR-001"
                )
        );

        verifyNoInteractions(
                documentRepository,
                verificationCheckRepository,
                manualReviewService,
                kycService,
                customerService,
                auditService
        );
    }

    @Test
    void shouldRejectWhenApplicationIsNotWaitingForStepUp() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createStepUpApplication(applicationId);

        application.setStatus(
                OnboardingStatus.STARTED
        );

        StepUpRequest request =
                new StepUpRequest(
                        "SOUTH_AFRICAN_ID",
                        "9001015000088",
                        "s3://nanobank/test-document"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.of(application));

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> stepUpService.submit(
                                applicationId,
                                request,
                                "CORR-002"
                        )
                );

        assertEquals(
                "Application is not waiting for step-up verification",
                exception.getMessage()
        );

        verifyNoInteractions(
                documentRepository,
                verificationCheckRepository,
                manualReviewService,
                kycService,
                customerService,
                auditService
        );
    }

    @Test
    void shouldRejectWhenApplicationIsNotCurrentlyAtStepUp() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createStepUpApplication(applicationId);

        application.setCurrentStep(
                OnboardingStep.IDENTITY_VERIFICATION
        );

        StepUpRequest request =
                new StepUpRequest(
                        "SOUTH_AFRICAN_ID",
                        "9001015000088",
                        "s3://nanobank/test-document"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.of(application));

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> stepUpService.submit(
                                applicationId,
                                request,
                                "CORR-003"
                        )
                );

        assertEquals(
                "Application is not currently at STEP_UP",
                exception.getMessage()
        );

        verifyNoInteractions(
                documentRepository,
                verificationCheckRepository,
                manualReviewService,
                kycService,
                customerService,
                auditService
        );
    }

    @Test
    void shouldRejectInvalidDocumentType() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createStepUpApplication(applicationId);

        StepUpRequest request =
                new StepUpRequest(
                        "INVALID_DOCUMENT",
                        "9001015000088",
                        "s3://nanobank/test-document"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.of(application));

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> stepUpService.submit(
                                applicationId,
                                request,
                                "CORR-004"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Invalid document type")
        );

        verify(documentRepository, never())
                .save(any());

        verifyNoInteractions(
                verificationCheckRepository,
                manualReviewService,
                kycService,
                customerService,
                auditService
        );
    }

    @Test
    void shouldSaveStepUpDocumentWithInProgressStatus() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createStepUpApplication(applicationId);

        StepUpRequest request =
                new StepUpRequest(
                        "SOUTH_AFRICAN_ID",
                        "9001015000088",
                        "s3://nanobank/document-123"
                );

        when(onboardingRepository.findById(applicationId))
                .thenReturn(java.util.Optional.of(application));

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Wayne")
                .lastName("Mokoena")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9001015000088")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerService.createFromOnboarding(application))
                .thenReturn(customer);

        ArgumentCaptor<OnboardingDocument> captor =
                ArgumentCaptor.forClass(
                        OnboardingDocument.class
                );

        stepUpService.submit(
                applicationId,
                request,
                "CORR-005"
        );

        verify(documentRepository)
                .save(captor.capture());

        OnboardingDocument document =
                captor.getValue();

        assertEquals(
                application,
                document.getOnboardingApplication()
        );

        assertEquals(
                IdentityDocumentType.SOUTH_AFRICAN_ID,
                document.getDocumentType()
        );

        assertEquals(
                "9001015000088",
                document.getDocumentNumber()
        );

        assertEquals(
                "s3://nanobank/document-123",
                document.getStorageReference()
        );

        assertEquals(
                VerificationStatus.IN_PROGRESS,
                document.getVerificationStatus()
        );
    }

    private OnboardingApplication createStepUpApplication(
            UUID applicationId
    ) {

        return OnboardingApplication.builder()
                .id(applicationId)
                .applicationReference("NB-STEPUP01")
                .status(OnboardingStatus.STEP_UP_REQUIRED)
                .currentStep(OnboardingStep.STEP_UP)
                .firstName("Wayne")
                .lastName("Mokoena")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9001015000088")
                .kycStatus(KycStatus.PENDING)
                .build();
    }
}