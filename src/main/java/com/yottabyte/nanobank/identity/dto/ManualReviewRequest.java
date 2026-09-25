package com.yottabyte.nanobank.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ManualReviewRequest(

        @NotBlank
        String reviewerId,

        @NotBlank
        String decision,

        String comment
) {
}
