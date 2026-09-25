package com.yottabyte.nanobank.identity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record StartOnboardingRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(max = 30)
        String mobileNumber,

        @NotBlank
        @Size(max = 50)
        String nationalId,

        @NotBlank
        String addressLine1,

        String addressLine2,

        @NotBlank
        String city,

        @NotBlank
        String province,

        @NotBlank
        String postalCode
) {
}
