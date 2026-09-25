package com.yottabyte.nanobank.identity.entity;

import com.yottabyte.nanobank.identity.enums.ManualReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_application_id", nullable = false, unique = true)
    private OnboardingApplication onboardingApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManualReviewStatus status;

    private String reason;

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "reviewer_comment")
    private String reviewerComment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
