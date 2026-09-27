package com.yottabyte.nanobank.identity.entity;

import com.yottabyte.nanobank.identity.enums.IdentityDocumentType;
import com.yottabyte.nanobank.identity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "onboarding_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_application_id", nullable = false)
    private OnboardingApplication onboardingApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private IdentityDocumentType documentType;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    @Column(name = "storage_reference")
    private String storageReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();

        if (verificationStatus == null) {
            verificationStatus = VerificationStatus.PENDING;
        }
    }
}
