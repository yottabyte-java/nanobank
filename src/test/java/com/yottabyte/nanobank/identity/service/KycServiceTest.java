package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.KycHistory;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.repository.KycHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycHistoryRepository kycHistoryRepository;

    @InjectMocks
    private KycService kycService;

    @Test
    void shouldChangeKycStatusAndCreateHistory() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .kycStatus(KycStatus.PENDING)
                        .build();

        kycService.changeStatus(
                application,
                KycStatus.ACCEPTED,
                "All verification checks passed."
        );

        assertEquals(
                KycStatus.ACCEPTED,
                application.getKycStatus()
        );

        ArgumentCaptor<KycHistory> captor =
                ArgumentCaptor.forClass(KycHistory.class);

        verify(kycHistoryRepository)
                .save(captor.capture());

        KycHistory history = captor.getValue();

        assertEquals(
                application,
                history.getOnboardingApplication()
        );

        assertEquals(
                KycStatus.PENDING,
                history.getPreviousStatus()
        );

        assertEquals(
                KycStatus.ACCEPTED,
                history.getNewStatus()
        );

        assertEquals(
                "All verification checks passed.",
                history.getReason()
        );
    }

    @Test
    void shouldRecordRejectedKycStatus() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .kycStatus(KycStatus.PENDING)
                        .build();

        kycService.changeStatus(
                application,
                KycStatus.REJECTED,
                "Fraud verification failed."
        );

        assertEquals(
                KycStatus.REJECTED,
                application.getKycStatus()
        );

        ArgumentCaptor<KycHistory> captor =
                ArgumentCaptor.forClass(KycHistory.class);

        verify(kycHistoryRepository)
                .save(captor.capture());

        KycHistory history = captor.getValue();

        assertEquals(
                application,
                history.getOnboardingApplication()
        );

        assertEquals(
                KycStatus.PENDING,
                history.getPreviousStatus()
        );

        assertEquals(
                KycStatus.REJECTED,
                history.getNewStatus()
        );

        assertEquals(
                "Fraud verification failed.",
                history.getReason()
        );
    }
}