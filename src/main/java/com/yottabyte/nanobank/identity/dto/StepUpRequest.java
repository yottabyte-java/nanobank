package com.yottabyte.nanobank.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record StepUpRequest(

        @NotBlank
        String documentType,

        @NotBlank
        String documentNumber,

        String storageReference
) {
}
