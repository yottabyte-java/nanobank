package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.entity.VerificationCheck;
import com.yottabyte.nanobank.identity.enums.Decision;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStatus;
import com.yottabyte.nanobank.identity.enums.OnboardingStep;
import com.yottabyte.nanobank.identity.enums.VerificationStatus;
import com.yottabyte.nanobank.identity.enums.VerificationType;
import com.yottabyte.nanobank.identity.provider.DocumentVerificationProvider;
import com.yottabyte.nanobank.identity.provider.FraudDetectionProvider;
import com.yottabyte.nanobank.identity.provider.IdentityVerificationProvider;
import com.yottabyte.nanobank.identity.provider.SanctionsScreeningProvider;
import com.yottabyte.nanobank.identity.provider.VerificationResult;
import com.yottabyte.nanobank.identity.repository.VerificationCheckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationOrchestratorTest {

    @Mock
    private IdentityVerificationProvider identityProvider;

    @Mock
    private DocumentVerificationProvider documentProvider;

    @Mock
    private FraudDetectionProvider fraudProvider;

    @Mock
    private SanctionsScreeningProvider sanctionsProvider;

    @Mock
    private VerificationCheckRepository verificationRepository;

    @InjectMocks
    private VerificationOrchestrator verificationOrchestrator;

    @Test
    void shouldExecuteAllVerificationProvidersAndReturnResults() {

        OnboardingApplication application =
                createApplication();

        VerificationResult identityResult =
                VerificationResult.pass(
                        "MOCK_IDENTITY",
                        "ID-001"
                );

        VerificationResult documentResult =
                VerificationResult.pass(
                        "MOCK_DOCUMENT",
                        "DOC-001"
                );

        VerificationResult fraudResult =
                VerificationResult.pass(
                        "MOCK_FRAUD",
                        "FRAUD-001"
                );

        VerificationResult sanctionsResult =
                VerificationResult.pass(
                        "MOCK_SANCTIONS",
                        "SANCTIONS-001"
                );

        when(identityProvider.verify(application))
                .thenReturn(identityResult);

        when(documentProvider.verify(application))
                .thenReturn(documentResult);

        when(fraudProvider.screen(application))
                .thenReturn(fraudResult);

        when(sanctionsProvider.screen(application))
                .thenReturn(sanctionsResult);

        List<VerificationResult> results =
                verificationOrchestrator.verify(application);

        assertEquals(4, results.size());

        assertEquals(
                identityResult,
                results.get(0)
        );

        assertEquals(
                documentResult,
                results.get(1)
        );

        assertEquals(
                fraudResult,
                results.get(2)
        );

        assertEquals(
                sanctionsResult,
                results.get(3)
        );

        verify(identityProvider)
                .verify(application);

        verify(documentProvider)
                .verify(application);

        verify(fraudProvider)
                .screen(application);

        verify(sanctionsProvider)
                .screen(application);

        verify(verificationRepository, times(4))
                .save(any(VerificationCheck.class));
    }

    @Test
    void shouldPersistPassedVerificationChecksAsCompleted() {

        OnboardingApplication application =
                createApplication();

        when(identityProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_IDENTITY",
                                "ID-001"
                        )
                );

        when(documentProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_DOCUMENT",
                                "DOC-001"
                        )
                );

        when(fraudProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_FRAUD",
                                "FRAUD-001"
                        )
                );

        when(sanctionsProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_SANCTIONS",
                                "SANCTIONS-001"
                        )
                );

        ArgumentCaptor<VerificationCheck> captor =
                ArgumentCaptor.forClass(
                        VerificationCheck.class
                );

        verificationOrchestrator.verify(application);

        verify(verificationRepository, times(4))
                .save(captor.capture());

        List<VerificationCheck> checks =
                captor.getAllValues();

        assertEquals(
                VerificationStatus.COMPLETED,
                checks.get(0).getStatus()
        );

        assertEquals(
                VerificationStatus.COMPLETED,
                checks.get(1).getStatus()
        );

        assertEquals(
                VerificationStatus.COMPLETED,
                checks.get(2).getStatus()
        );

        assertEquals(
                VerificationStatus.COMPLETED,
                checks.get(3).getStatus()
        );
    }

    @Test
    void shouldPersistFailedVerificationAsFailed() {

        OnboardingApplication application =
                createApplication();

        when(identityProvider.verify(application))
                .thenReturn(
                        VerificationResult.fail(
                                "MOCK_IDENTITY",
                                "ID-FAIL-001",
                                "Identity could not be verified"
                        )
                );

        when(documentProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_DOCUMENT",
                                "DOC-001"
                        )
                );

        when(fraudProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_FRAUD",
                                "FRAUD-001"
                        )
                );

        when(sanctionsProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_SANCTIONS",
                                "SANCTIONS-001"
                        )
                );

        ArgumentCaptor<VerificationCheck> captor =
                ArgumentCaptor.forClass(
                        VerificationCheck.class
                );

        verificationOrchestrator.verify(application);

        verify(verificationRepository, times(4))
                .save(captor.capture());

        VerificationCheck identityCheck =
                captor.getAllValues().get(0);

        assertEquals(
                VerificationType.IDENTITY,
                identityCheck.getVerificationType()
        );

        assertEquals(
                VerificationStatus.FAILED,
                identityCheck.getStatus()
        );

        assertEquals(
                Decision.FAIL,
                identityCheck.getDecision()
        );

        assertEquals(
                "MOCK_IDENTITY",
                identityCheck.getProvider()
        );

        assertEquals(
                "ID-FAIL-001",
                identityCheck.getProviderReference()
        );

        assertEquals(
                "Identity could not be verified",
                identityCheck.getFailureReason()
        );
    }

    @Test
    void shouldPersistReferVerificationAsCompleted() {

        OnboardingApplication application =
                createApplication();

        when(identityProvider.verify(application))
                .thenReturn(
                        VerificationResult.refer(
                                "MOCK_IDENTITY",
                                "ID-REFER-001",
                                "Additional verification required"
                        )
                );

        when(documentProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_DOCUMENT",
                                "DOC-001"
                        )
                );

        when(fraudProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_FRAUD",
                                "FRAUD-001"
                        )
                );

        when(sanctionsProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "MOCK_SANCTIONS",
                                "SANCTIONS-001"
                        )
                );

        ArgumentCaptor<VerificationCheck> captor =
                ArgumentCaptor.forClass(
                        VerificationCheck.class
                );

        verificationOrchestrator.verify(application);

        verify(verificationRepository, times(4))
                .save(captor.capture());

        VerificationCheck identityCheck =
                captor.getAllValues().get(0);

        assertEquals(
                VerificationType.IDENTITY,
                identityCheck.getVerificationType()
        );

        assertEquals(
                VerificationStatus.COMPLETED,
                identityCheck.getStatus()
        );

        assertEquals(
                Decision.REFER,
                identityCheck.getDecision()
        );

        assertEquals(
                "Additional verification required",
                identityCheck.getFailureReason()
        );
    }

    @Test
    void shouldPersistVerificationTypesInCorrectOrder() {

        OnboardingApplication application =
                createApplication();

        when(identityProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "IDENTITY_PROVIDER",
                                "ID-001"
                        )
                );

        when(documentProvider.verify(application))
                .thenReturn(
                        VerificationResult.pass(
                                "DOCUMENT_PROVIDER",
                                "DOC-001"
                        )
                );

        when(fraudProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "FRAUD_PROVIDER",
                                "FRAUD-001"
                        )
                );

        when(sanctionsProvider.screen(application))
                .thenReturn(
                        VerificationResult.pass(
                                "SANCTIONS_PROVIDER",
                                "SANCTIONS-001"
                        )
                );

        ArgumentCaptor<VerificationCheck> captor =
                ArgumentCaptor.forClass(
                        VerificationCheck.class
                );

        verificationOrchestrator.verify(application);

        verify(verificationRepository, times(4))
                .save(captor.capture());

        List<VerificationCheck> checks =
                captor.getAllValues();

        assertEquals(
                VerificationType.IDENTITY,
                checks.get(0).getVerificationType()
        );

        assertEquals(
                VerificationType.DOCUMENT,
                checks.get(1).getVerificationType()
        );

        assertEquals(
                VerificationType.FRAUD,
                checks.get(2).getVerificationType()
        );

        assertEquals(
                VerificationType.SANCTIONS,
                checks.get(3).getVerificationType()
        );
    }

    private OnboardingApplication createApplication() {

        return OnboardingApplication.builder()
                .id(UUID.randomUUID())
                .applicationReference("NB-VERIFY01")
                .status(OnboardingStatus.VERIFICATION_IN_PROGRESS)
                .currentStep(OnboardingStep.IDENTITY_VERIFICATION)
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