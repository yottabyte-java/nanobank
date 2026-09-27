package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.KycHistory;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.KycStatus;
import com.yottabyte.nanobank.identity.repository.KycHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycHistoryRepository kycHistoryRepository;

    @Transactional
    public void changeStatus(
            OnboardingApplication application,
            KycStatus newStatus,
            String reason
    ) {

        KycStatus previousStatus = application.getKycStatus();

        application.setKycStatus(newStatus);

        KycHistory history = KycHistory.builder()
                .onboardingApplication(application)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();

        kycHistoryRepository.save(history);
    }
}
