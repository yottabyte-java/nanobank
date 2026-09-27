package com.yottabyte.nanobank.identity.entity;

import com.yottabyte.nanobank.identity.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_application_id", nullable = false)
    private OnboardingApplication onboardingApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private KycStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private KycStatus newStatus;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
