CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE customers (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           first_name VARCHAR(100) NOT NULL,
                           last_name VARCHAR(100) NOT NULL,

                           date_of_birth DATE NOT NULL,

                           email VARCHAR(255) NOT NULL,
                           mobile_number VARCHAR(30) NOT NULL,


                           national_id VARCHAR(50) NOT NULL,

                           status VARCHAR(30) NOT NULL,

                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,

                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT uk_customer_email UNIQUE (email),
                           CONSTRAINT uk_customer_mobile UNIQUE (mobile_number),
                           CONSTRAINT uk_customer_national_id UNIQUE (national_id)
);

CREATE TABLE addresses (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                           customer_id UUID NOT NULL,

                           address_line_1 VARCHAR(255) NOT NULL,
                           address_line_2 VARCHAR(255),
                           city VARCHAR(100) NOT NULL,
                           province VARCHAR(100) NOT NULL,
                           postal_code VARCHAR(20) NOT NULL,
                           country VARCHAR(100) NOT NULL,

                           CONSTRAINT fk_address_customer
                               FOREIGN KEY (customer_id)
                                   REFERENCES customers(id)
);

CREATE TABLE identity_documents (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                    customer_id UUID NOT NULL,

                                    document_type VARCHAR(50) NOT NULL,
                                    document_number VARCHAR(100) NOT NULL,
                                    country_of_issue VARCHAR(100) NOT NULL,
                                    expiry_date DATE,

                                    verification_status VARCHAR(30) NOT NULL,

    -- Reference to external/object storage.

                                    storage_reference VARCHAR(500),

                                    created_at TIMESTAMPTZ NOT NULL,

                                    CONSTRAINT fk_identity_document_customer
                                        FOREIGN KEY (customer_id)
                                            REFERENCES customers(id)
);

CREATE TABLE onboarding_applications (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                         application_reference VARCHAR(50) NOT NULL,

                                         status VARCHAR(50) NOT NULL,
                                         current_step VARCHAR(50) NOT NULL,

                                         customer_id UUID,

                                         first_name VARCHAR(100) NOT NULL,
                                         last_name VARCHAR(100) NOT NULL,
                                         date_of_birth DATE NOT NULL,
                                         email VARCHAR(255) NOT NULL,
                                         mobile_number VARCHAR(30) NOT NULL,
                                         national_id VARCHAR(50) NOT NULL,

                                         kyc_status VARCHAR(30) NOT NULL,

                                         created_at TIMESTAMPTZ NOT NULL,
                                         updated_at TIMESTAMPTZ NOT NULL,

                                         version BIGINT NOT NULL DEFAULT 0,

                                         CONSTRAINT uk_application_reference
                                             UNIQUE (application_reference)
);

CREATE INDEX idx_onboarding_email
    ON onboarding_applications(email);

CREATE INDEX idx_onboarding_status
    ON onboarding_applications(status);

CREATE TABLE verification_checks (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     onboarding_application_id UUID NOT NULL,

                                     verification_type VARCHAR(50) NOT NULL,
                                     status VARCHAR(30) NOT NULL,
                                     decision VARCHAR(30),

                                     provider VARCHAR(100) NOT NULL,
                                     provider_reference VARCHAR(255),

                                     failure_reason VARCHAR(500),

                                     created_at TIMESTAMPTZ NOT NULL,
                                     completed_at TIMESTAMPTZ,

                                     CONSTRAINT fk_verification_application
                                         FOREIGN KEY (onboarding_application_id)
                                             REFERENCES onboarding_applications(id)
);

CREATE INDEX idx_verification_application
    ON verification_checks(onboarding_application_id);

CREATE TABLE kyc_history (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             onboarding_application_id UUID NOT NULL,

                             previous_status VARCHAR(30),
                             new_status VARCHAR(30) NOT NULL,

                             reason VARCHAR(500),

                             created_at TIMESTAMPTZ NOT NULL,

                             CONSTRAINT fk_kyc_history_application
                                 FOREIGN KEY (onboarding_application_id)
                                     REFERENCES onboarding_applications(id)
);

CREATE TABLE manual_reviews (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                onboarding_application_id UUID NOT NULL,

                                status VARCHAR(30) NOT NULL,

                                reason VARCHAR(500),

                                reviewer_id VARCHAR(100),
                                reviewer_comment VARCHAR(2000),

                                created_at TIMESTAMPTZ NOT NULL,
                                completed_at TIMESTAMPTZ,

                                CONSTRAINT fk_manual_review_application
                                    FOREIGN KEY (onboarding_application_id)
                                        REFERENCES onboarding_applications(id)
);

CREATE TABLE audit_events (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              entity_type VARCHAR(100) NOT NULL,
                              entity_id UUID NOT NULL,

                              event_type VARCHAR(100) NOT NULL,

                              actor_type VARCHAR(50),
                              actor_id VARCHAR(100),

                              correlation_id VARCHAR(100),

                              metadata TEXT,

                              created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_entity
    ON audit_events(entity_type, entity_id);

CREATE TABLE idempotency_records (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     idempotency_key VARCHAR(255) NOT NULL,
                                     operation VARCHAR(100) NOT NULL,

                                     request_hash VARCHAR(128) NOT NULL,

                                     response_status INTEGER,
                                     response_body TEXT,

                                     created_at TIMESTAMPTZ NOT NULL,
                                     expires_at TIMESTAMPTZ,

                                     CONSTRAINT uk_idempotency_key
                                         UNIQUE (idempotency_key)
);

CREATE TABLE onboarding_documents (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      onboarding_application_id UUID NOT NULL,

                                      document_type VARCHAR(50) NOT NULL,
                                      document_number VARCHAR(100) NOT NULL,
                                      storage_reference VARCHAR(500),

                                      verification_status VARCHAR(30) NOT NULL,

                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_onboarding_document_application
                                          FOREIGN KEY (onboarding_application_id)
                                              REFERENCES onboarding_applications(id)
);

CREATE INDEX idx_onboarding_documents_application
    ON onboarding_documents(onboarding_application_id);