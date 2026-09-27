package com.yottabyte.nanobank.identity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest
class IdentityDatabaseMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateFreshDatabaseUsingFlyway() {

        Integer migrationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        """,
                        Integer.class
                );

        assertNotNull(migrationCount);

        assertEquals(
                1,
                migrationCount
        );

        Integer customersTable =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'customers'
                        """,
                        Integer.class
                );

        Integer onboardingTable =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'onboarding_applications'
                        """,
                        Integer.class
                );

        Integer auditTable =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'audit_events'
                        """,
                        Integer.class
                );

        Integer idempotencyTable =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'idempotency_records'
                        """,
                        Integer.class
                );

        assertEquals(1, customersTable);
        assertEquals(1, onboardingTable);
        assertEquals(1, auditTable);
        assertEquals(1, idempotencyTable);
    }
}