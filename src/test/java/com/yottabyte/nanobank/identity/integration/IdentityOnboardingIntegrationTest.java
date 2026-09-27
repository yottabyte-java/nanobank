package com.yottabyte.nanobank.identity.integration;

import com.yottabyte.nanobank.identity.provider.DocumentVerificationProvider;
import com.yottabyte.nanobank.identity.provider.FraudDetectionProvider;
import com.yottabyte.nanobank.identity.provider.IdentityVerificationProvider;
import com.yottabyte.nanobank.identity.provider.SanctionsScreeningProvider;
import com.yottabyte.nanobank.identity.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class IdentityOnboardingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private IdentityVerificationProvider identityVerificationProvider;

    @MockitoBean
    private DocumentVerificationProvider documentVerificationProvider;

    @MockitoBean
    private FraudDetectionProvider fraudDetectionProvider;

    @MockitoBean
    private SanctionsScreeningProvider sanctionsScreeningProvider;

    @MockitoBean
    private AddressService addressService;

    @Test
    void shouldCompleteFullOnboardingPassFlow() throws Exception {

        when(identityVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(documentVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(fraudDetectionProvider.screen(any()))
                .thenReturn(pass());

        when(sanctionsScreeningProvider.screen(any()))
                .thenReturn(pass());

        String request = """
                {
                  "firstName": "Wayne",
                  "lastName": "Integration",
                  "dateOfBirth": "1998-05-10",
                  "email": "wayne.integration.pass@example.com",
                  "mobileNumber": "0821234501",
                  "nationalId": "9805105000001",
                  "addressLine1": "123 Integration Street",
                  "addressLine2": "",
                  "city": "Polokwane",
                  "province": "Limpopo",
                  "postalCode": "0700"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/onboarding")
                                .header(
                                        "Idempotency-Key",
                                        "integration-pass-001"
                                )
                                .header(
                                        "X-Correlation-ID",
                                        "integration-correlation-001"
                                )
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId",
                        notNullValue()))
                .andExpect(jsonPath("$.status",
                        containsString("CUSTOMER_CREATED")))
                .andExpect(jsonPath("$.currentStep",
                        containsString("COMPLETED")))
                .andExpect(jsonPath("$.kycStatus",
                        containsString("ACCEPTED")))
                .andExpect(header().string(
                        "X-Correlation-ID",
                        "integration-correlation-001"
                ));

        Integer customers =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM customers
                        WHERE email = ?
                        """,
                        Integer.class,
                        "wayne.integration.pass@example.com"
                );

        Integer applications =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM onboarding_applications
                        WHERE email = ?
                        """,
                        Integer.class,
                        "wayne.integration.pass@example.com"
                );

        Integer idempotencyRecords =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM idempotency_records
                        WHERE idempotency_key = ?
                        """,
                        Integer.class,
                        "integration-pass-001"
                );

        assert customers != null;
        assert applications != null;
        assert idempotencyRecords != null;

        org.junit.jupiter.api.Assertions.assertEquals(1, customers);
        org.junit.jupiter.api.Assertions.assertEquals(1, applications);
        org.junit.jupiter.api.Assertions.assertEquals(1, idempotencyRecords);
    }

    @Test
    void shouldCompleteFullOnboardingFailFlow() throws Exception {

        when(identityVerificationProvider.verify(any()))
                .thenReturn(fail());

        when(documentVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(fraudDetectionProvider.screen(any()))
                .thenReturn(pass());

        when(sanctionsScreeningProvider.screen(any()))
                .thenReturn(pass());

        String request = """
                {
                  "firstName": "Wayne",
                  "lastName": "IntegrationFail",
                  "dateOfBirth": "1998-05-10",
                  "email": "wayne.integration.fail@example.com",
                  "mobileNumber": "0821234502",
                  "nationalId": "9805105000002",
                  "addressLine1": "456 Failure Street",
                  "addressLine2": "",
                  "city": "Polokwane",
                  "province": "Limpopo",
                  "postalCode": "0700"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/onboarding")
                                .header(
                                        "Idempotency-Key",
                                        "integration-fail-001"
                                )
                                .header(
                                        "X-Correlation-ID",
                                        "integration-correlation-fail"
                                )
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("REJECTED"))
                .andExpect(jsonPath("$.currentStep")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.kycStatus")
                        .value("REJECTED"));

        Integer customers =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM customers
                        WHERE email = ?
                        """,
                        Integer.class,
                        "wayne.integration.fail@example.com"
                );

        Integer rejectedApplications =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM onboarding_applications
                        WHERE email = ?
                          AND status = 'REJECTED'
                        """,
                        Integer.class,
                        "wayne.integration.fail@example.com"
                );

        assert customers != null;
        assert rejectedApplications != null;

        org.junit.jupiter.api.Assertions.assertEquals(0, customers);
        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                rejectedApplications
        );
    }

    @Test
    void shouldMoveToStepUpWhenOnboardingIsReferred() throws Exception {

        when(identityVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(documentVerificationProvider.verify(any()))
                .thenReturn(refer());

        when(fraudDetectionProvider.screen(any()))
                .thenReturn(pass());

        when(sanctionsScreeningProvider.screen(any()))
                .thenReturn(pass());

        String request = """
            {
              "firstName": "Wayne",
              "lastName": "IntegrationRefer",
              "dateOfBirth": "1998-05-10",
              "email": "wayne.integration.refer@example.com",
              "mobileNumber": "0821234503",
              "nationalId": "9805105000003",
              "addressLine1": "789 Refer Street",
              "addressLine2": "",
              "city": "Polokwane",
              "province": "Limpopo",
              "postalCode": "0700"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/onboarding")
                                .header(
                                        "Idempotency-Key",
                                        "integration-refer-001"
                                )
                                .header(
                                        "X-Correlation-ID",
                                        "integration-correlation-refer"
                                )
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("STEP_UP_REQUIRED"))
                .andExpect(jsonPath("$.currentStep")
                        .value("STEP_UP"))
                .andExpect(jsonPath("$.kycStatus")
                        .value("PENDING"));

        Integer applications =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM onboarding_applications
                        WHERE email = ?
                          AND status = 'STEP_UP_REQUIRED'
                        """,
                        Integer.class,
                        "wayne.integration.refer@example.com"
                );

        assert applications != null;

        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                applications
        );
    }

    @Test
    void shouldRollbackOnboardingWhenDatabaseFailureOccurs() throws Exception {

        when(identityVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(documentVerificationProvider.verify(any()))
                .thenReturn(pass());

        when(fraudDetectionProvider.screen(any()))
                .thenReturn(pass());

        when(sanctionsScreeningProvider.screen(any()))
                .thenReturn(pass());

        org.mockito.Mockito.doThrow(
                        new RuntimeException("Simulated database failure")
                )
                .when(addressService)
                .createForCustomer(any(), any());

        String email =
                "wayne.integration.rollback@example.com";

        String request = """
            {
              "firstName": "Wayne",
              "lastName": "Rollback",
              "dateOfBirth": "1998-05-10",
              "email": "%s",
              "mobileNumber": "0821234504",
              "nationalId": "9805105000004",
              "addressLine1": "Rollback Street",
              "addressLine2": "",
              "city": "Polokwane",
              "province": "Limpopo",
              "postalCode": "0700"
            }
            """.formatted(email);

        mockMvc.perform(
                        post("/api/v1/onboarding")
                                .header(
                                        "Idempotency-Key",
                                        "integration-rollback-001"
                                )
                                .header(
                                        "X-Correlation-ID",
                                        "integration-rollback-correlation"
                                )
                                .contentType("application/json")
                                .content(request)
                )
                .andExpect(status().is5xxServerError());

        Integer customerCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM customers
                        WHERE email = ?
                        """,
                        Integer.class,
                        email
                );

        Integer applicationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM onboarding_applications
                        WHERE email = ?
                        """,
                        Integer.class,
                        email
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                customerCount
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                0,
                applicationCount
        );
    }

    private com.yottabyte.nanobank.identity.provider.VerificationResult pass() {
        return new com.yottabyte.nanobank.identity.provider.VerificationResult(
                com.yottabyte.nanobank.identity.enums.Decision.PASS,
                "INTEGRATION_TEST_PROVIDER",
                "integration-reference",
                null
        );
    }

    private com.yottabyte.nanobank.identity.provider.VerificationResult fail() {

        return new com.yottabyte.nanobank.identity.provider.VerificationResult(
                com.yottabyte.nanobank.identity.enums.Decision.FAIL,
                "INTEGRATION_TEST_PROVIDER",
                "integration-failure-reference",
                "Integration test verification failure"
        );
    }

    private com.yottabyte.nanobank.identity.provider.VerificationResult refer() {

        return new com.yottabyte.nanobank.identity.provider.VerificationResult(
                com.yottabyte.nanobank.identity.enums.Decision.REFER,
                "INTEGRATION_TEST_PROVIDER",
                "integration-refer-reference",
                "Additional verification required"
        );
    }
}