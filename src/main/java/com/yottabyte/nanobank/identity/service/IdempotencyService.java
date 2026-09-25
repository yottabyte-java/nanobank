package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.OnboardingResponse;
import com.yottabyte.nanobank.identity.entity.IdempotencyRecord;
import com.yottabyte.nanobank.identity.exception.InvalidOnboardingStateException;
import com.yottabyte.nanobank.identity.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public String createRequestHash(Object request) {

        try {

            String json =
                    objectMapper.writeValueAsString(request);

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            json.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (
                JacksonException |
                NoSuchAlgorithmException exception
        ) {

            throw new IllegalStateException(
                    "Unable to generate request hash.",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public OnboardingResponse findExisting(
            String idempotencyKey,
            String requestHash
    ) {

        return repository
                .findByIdempotencyKey(idempotencyKey)
                .map(record -> {

                    if (!record.getRequestHash()
                            .equals(requestHash)) {

                        throw new InvalidOnboardingStateException(
                                "Idempotency key was already used with a different request."
                        );
                    }

                    if (record.getResponseBody() == null) {

                        throw new IllegalStateException(
                                "Idempotency record has no stored response."
                        );
                    }

                    try {

                        return objectMapper.readValue(
                                record.getResponseBody(),
                                OnboardingResponse.class
                        );

                    } catch (JacksonException exception) {

                        throw new IllegalStateException(
                                "Unable to restore idempotent response.",
                                exception
                        );
                    }
                })
                .orElse(null);
    }

    @Transactional
    public void saveResponse(
            String idempotencyKey,
            String operation,
            String requestHash,
            OnboardingResponse response
    ) {

        try {

            String responseBody =
                    objectMapper.writeValueAsString(response);

            IdempotencyRecord record =
                    IdempotencyRecord.builder()
                            .idempotencyKey(idempotencyKey)
                            .operation(operation)
                            .requestHash(requestHash)
                            .responseStatus(200)
                            .responseBody(responseBody)
                            .createdAt(Instant.now())
                            .build();

            repository.save(record);

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                    "Unable to save idempotent response.",
                    exception
            );
        }
    }
}