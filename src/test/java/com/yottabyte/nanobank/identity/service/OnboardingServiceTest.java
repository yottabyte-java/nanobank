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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private AddressService addressService;

    @Mock
    private OnboardingApplicationRepository applicationRepository;

    @Mock
    private VerificationOrchestrator verificationOrchestrator;

    @Mock
    private CustomerService customerService;

    @Mock
    private KycService kycService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OnboardingService onboardingService;

    private static final String IDEMPOTENCY_KEY =
            "test-idempotency-key";

    private static final String CORRELATION_ID =
            "test-correlation-id";

    @Test
    void shouldCreateCustomerWhenAllVerificationChecksPass() {

        StartOnboardingRequest request = createRequest();

        when(idempotencyService.createRequestHash(request))
                .thenReturn("request-hash");

        when(idempotencyService.findExisting(
                IDEMPOTENCY_KEY,
                "request-hash"
        )).thenReturn(null);

        when(applicationRepository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        when(applicationRepository.existsByNationalId(request.nationalId()))
                .thenReturn(false);

        OnboardingApplication application = createApplication();

        when(applicationRepository.save(any(OnboardingApplication.class)))
                .thenReturn(application);

        VerificationResult passResult =
                new VerificationResult(
                        Decision.PASS,
                        "MOCK_PROVIDER",
                        "provider-ref-1",
                        null
                );

        when(verificationOrchestrator.verify(application))
                .thenReturn(List.of(
                        passResult,
                        passResult,
                        passResult,
                        passResult
                ));

        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .firstName("Wayne")
                .lastName("Mokoena")
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9805105000088")
                .status(CustomerStatus.ACTIVE)
                .build();

        when(customerService.createFromOnboarding(application))
                .thenReturn(customer);

        OnboardingResponse response =
                onboardingService.start(
                        request,
                        IDEMPOTENCY_KEY,
                        CORRELATION_ID
                );

        assertNotNull(response);

        assertEquals(
                OnboardingStatus.CUSTOMER_CREATED,
                response.status()
        );

        assertEquals(
                OnboardingStep.COMPLETED,
                response.currentStep()
        );

        verify(customerService)
                .createFromOnboarding(application);

        verify(addressService)
                .createForCustomer(customer, request);

        verify(kycService)
                .changeStatus(
                        application,
                        KycStatus.ACCEPTED,
                        "All automated checks passed."
                );

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.ONBOARDING_APPROVED),
                        eq(CORRELATION_ID),
                        eq("Customer onboarding approved.")
                );

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.CUSTOMER_CREATED),
                        eq(CORRELATION_ID),
                        eq("Customer created.")
                );

        verify(idempotencyService)
                .saveResponse(
                        eq(IDEMPOTENCY_KEY),
                        eq("START_ONBOARDING"),
                        eq("request-hash"),
                        eq(response)
                );
    }

    @Test
    void shouldRejectWhenVerificationFails() {

        StartOnboardingRequest request = createRequest();

        when(idempotencyService.createRequestHash(request))
                .thenReturn("request-hash");

        when(idempotencyService.findExisting(
                IDEMPOTENCY_KEY,
                "request-hash"
        )).thenReturn(null);

        when(applicationRepository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        when(applicationRepository.existsByNationalId(request.nationalId()))
                .thenReturn(false);

        OnboardingApplication application = createApplication();

        when(applicationRepository.save(any(OnboardingApplication.class)))
                .thenReturn(application);

        VerificationResult failResult =
                new VerificationResult(
                        Decision.FAIL,
                        "MOCK_PROVIDER",
                        "provider-ref-fail",
                        "Identity verification failed"
                );

        VerificationResult passResult =
                new VerificationResult(
                        Decision.PASS,
                        "MOCK_PROVIDER",
                        "provider-ref-pass",
                        null
                );

        when(verificationOrchestrator.verify(application))
                .thenReturn(List.of(
                        failResult,
                        passResult,
                        passResult,
                        passResult
                ));

        OnboardingResponse response =
                onboardingService.start(
                        request,
                        IDEMPOTENCY_KEY,
                        CORRELATION_ID
                );

        assertNotNull(response);

        assertEquals(
                OnboardingStatus.REJECTED,
                response.status()
        );

        assertEquals(
                OnboardingStep.COMPLETED,
                response.currentStep()
        );

        verify(customerService, never())
                .createFromOnboarding(any());

        verify(kycService)
                .changeStatus(
                        application,
                        KycStatus.REJECTED,
                        "Automated verification failed."
                );

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.ONBOARDING_REJECTED),
                        eq(CORRELATION_ID),
                        eq("Automated verification failed.")
                );

        verify(idempotencyService)
                .saveResponse(
                        eq(IDEMPOTENCY_KEY),
                        eq("START_ONBOARDING"),
                        eq("request-hash"),
                        eq(response)
                );
    }

    @Test
    void shouldRequireStepUpWhenVerificationIsReferred() {

        StartOnboardingRequest request = createRequest();

        when(idempotencyService.createRequestHash(request))
                .thenReturn("request-hash");

        when(idempotencyService.findExisting(
                IDEMPOTENCY_KEY,
                "request-hash"
        )).thenReturn(null);

        when(applicationRepository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        when(applicationRepository.existsByNationalId(request.nationalId()))
                .thenReturn(false);

        OnboardingApplication application = createApplication();

        when(applicationRepository.save(any(OnboardingApplication.class)))
                .thenReturn(application);

        VerificationResult referResult =
                new VerificationResult(
                        Decision.REFER,
                        "MOCK_PROVIDER",
                        "provider-ref-refer",
                        "Additional verification required"
                );

        VerificationResult passResult =
                new VerificationResult(
                        Decision.PASS,
                        "MOCK_PROVIDER",
                        "provider-ref-pass",
                        null
                );

        when(verificationOrchestrator.verify(application))
                .thenReturn(List.of(
                        passResult,
                        referResult,
                        passResult,
                        passResult
                ));

        OnboardingResponse response =
                onboardingService.start(
                        request,
                        IDEMPOTENCY_KEY,
                        CORRELATION_ID
                );

        assertNotNull(response);

        assertEquals(
                OnboardingStatus.STEP_UP_REQUIRED,
                response.status()
        );

        assertEquals(
                OnboardingStep.STEP_UP,
                response.currentStep()
        );

        assertEquals(
                KycStatus.PENDING,
                response.kycStatus()
        );

        verify(customerService, never())
                .createFromOnboarding(any());

        verify(kycService, never())
                .changeStatus(
                        any(),
                        any(),
                        anyString()
                );

        verify(auditService)
                .record(
                        eq("ONBOARDING_APPLICATION"),
                        eq(application.getId()),
                        eq(AuditEventType.STEP_UP_REQUESTED),
                        eq(CORRELATION_ID),
                        eq("Additional verification required.")
                );

        verify(idempotencyService)
                .saveResponse(
                        eq(IDEMPOTENCY_KEY),
                        eq("START_ONBOARDING"),
                        eq("request-hash"),
                        eq(response)
                );
    }

    @Test
    void shouldRejectWhenEmailAlreadyHasOnboardingApplication() {

        StartOnboardingRequest request = createRequest();

        when(idempotencyService.createRequestHash(request))
                .thenReturn("request-hash");

        when(idempotencyService.findExisting(
                IDEMPOTENCY_KEY,
                "request-hash"
        )).thenReturn(null);

        when(applicationRepository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(true);

        InvalidOnboardingStateException exception =
                assertThrows(
                        InvalidOnboardingStateException.class,
                        () -> onboardingService.start(
                                request,
                                IDEMPOTENCY_KEY,
                                CORRELATION_ID
                        )
                );

        assertEquals(
                "An onboarding application already exists for this identity.",
                exception.getMessage()
        );

        verify(applicationRepository, never())
                .save(any(OnboardingApplication.class));

        verify(verificationOrchestrator, never())
                .verify(any());

        verify(idempotencyService, never())
                .saveResponse(
                        anyString(),
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void shouldRejectWhenNationalIdAlreadyHasOnboardingApplication() {

        StartOnboardingRequest request = createRequest();

        when(idempotencyService.createRequestHash(request))
                .thenReturn("request-hash");

        when(idempotencyService.findExisting(
                IDEMPOTENCY_KEY,
                "request-hash"
        )).thenReturn(null);

        when(applicationRepository.existsByEmailIgnoreCase(request.email()))
                .thenReturn(false);

        when(applicationRepository.existsByNationalId(request.nationalId()))
                .thenReturn(true);

        assertThrows(
                InvalidOnboardingStateException.class,
                () -> onboardingService.start(
                        request,
                        IDEMPOTENCY_KEY,
                        CORRELATION_ID
                )
        );

        verify(applicationRepository, never())
                .save(any(OnboardingApplication.class));

        verify(verificationOrchestrator, never())
                .verify(any());

        verify(idempotencyService, never())
                .saveResponse(
                        anyString(),
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void shouldReturnOnboardingApplicationWhenFound() {

        UUID applicationId = UUID.randomUUID();

        OnboardingApplication application =
                createApplication();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        OnboardingResponse response =
                onboardingService.get(applicationId);

        assertNotNull(response);

        assertEquals(
                application.getId(),
                response.applicationId()
        );

        assertEquals(
                application.getApplicationReference(),
                response.applicationReference()
        );

        assertEquals(
                application.getStatus(),
                response.status()
        );
    }

    @Test
    void shouldThrowExceptionWhenOnboardingApplicationDoesNotExist() {

        UUID applicationId = UUID.randomUUID();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.empty());

        assertThrows(
                OnboardingNotFoundException.class,
                () -> onboardingService.get(applicationId)
        );
    }

    private StartOnboardingRequest createRequest() {

        return new StartOnboardingRequest(
                "Wayne",
                "Mokoena",
                LocalDate.of(1998, 5, 10),
                "wayne@example.com",
                "0821234567",
                "9805105000088",
                "123 Church Street",
                "",
                "Polokwane",
                "Limpopo",
                "0700"
        );
    }

    private OnboardingApplication createApplication() {

        return OnboardingApplication.builder()
                .id(UUID.randomUUID())
                .applicationReference("NB-TEST123")
                .status(OnboardingStatus.STARTED)
                .currentStep(OnboardingStep.CUSTOMER_INFORMATION)
                .firstName("Wayne")
                .lastName("Mokoena")
                .dateOfBirth(LocalDate.of(1998, 5, 10))
                .email("wayne@example.com")
                .mobileNumber("0821234567")
                .nationalId("9805105000088")
                .kycStatus(KycStatus.PENDING)
                .build();
    }
}