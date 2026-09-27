package com.yottabyte.nanobank.identity.provider;

import com.yottabyte.nanobank.identity.enums.Decision;

public record VerificationResult(

        Decision decision,

        String provider,

        String providerReference,

        String reason
) {

    public static VerificationResult pass(
            String provider,
            String reference
    ) {
        return new VerificationResult(
                Decision.PASS,
                provider,
                reference,
                null
        );
    }

    public static VerificationResult fail(
            String provider,
            String reference,
            String reason
    ) {
        return new VerificationResult(
                Decision.FAIL,
                provider,
                reference,
                reason
        );
    }

    public static VerificationResult refer(
            String provider,
            String reference,
            String reason
    ) {
        return new VerificationResult(
                Decision.REFER,
                provider,
                reference,
                reason
        );
    }
}
