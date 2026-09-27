package com.yottabyte.nanobank.identity.controller;

import com.yottabyte.nanobank.identity.dto.ManualReviewRequest;
import com.yottabyte.nanobank.identity.dto.ManualReviewResponse;
import com.yottabyte.nanobank.identity.dto.OnboardingResponse;
import com.yottabyte.nanobank.identity.dto.StartOnboardingRequest;
import com.yottabyte.nanobank.identity.dto.StepUpRequest;
import com.yottabyte.nanobank.identity.dto.StepUpResponse;
import com.yottabyte.nanobank.identity.service.ManualReviewService;
import com.yottabyte.nanobank.identity.service.OnboardingService;
import com.yottabyte.nanobank.identity.service.StepUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final StepUpService stepUpService;
    private final ManualReviewService manualReviewService;

    @PostMapping
    public ResponseEntity<OnboardingResponse> start(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false)
            String correlationId,
            @Valid @RequestBody StartOnboardingRequest request
    ) {

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        OnboardingResponse response =
                onboardingService.start(
                        request,
                        idempotencyKey,
                        correlationId
                );

        return ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .body(response);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<OnboardingResponse> get(
            @PathVariable UUID applicationId
    ) {

        return ResponseEntity.ok(
                onboardingService.get(applicationId)
        );
    }

    @PostMapping("/{applicationId}/step-up")
    public ResponseEntity<StepUpResponse> stepUp(
            @PathVariable UUID applicationId,
            @Valid @RequestBody StepUpRequest request
    ) {

        return ResponseEntity.ok(
                stepUpService.submit(
                        applicationId,
                        request,
                        null
                )
        );
    }

    @PostMapping("/{applicationId}/review")
    public ResponseEntity<ManualReviewResponse> review(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ManualReviewRequest request
    ) {

        return ResponseEntity.ok(
                manualReviewService.review(
                        applicationId,
                        request,
                        null
                )
        );
    }
}