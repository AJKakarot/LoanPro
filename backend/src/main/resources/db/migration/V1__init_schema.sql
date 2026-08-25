CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_status ON users (status);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE customer_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender VARCHAR(20),
    national_id VARCHAR(40),
    address_line VARCHAR(255),
    city VARCHAR(80),
    state VARCHAR(80),
    postal_code VARCHAR(20),
    employment_type VARCHAR(30),
    employer_name VARCHAR(160),
    designation VARCHAR(120),
    years_employed INTEGER,
    monthly_income NUMERIC(15, 2),
    other_income NUMERIC(15, 2),
    existing_emis NUMERIC(15, 2),
    monthly_expenses NUMERIC(15, 2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE loan_products (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    min_amount NUMERIC(15, 2) NOT NULL,
    max_amount NUMERIC(15, 2) NOT NULL,
    min_tenure_months INTEGER NOT NULL,
    max_tenure_months INTEGER NOT NULL,
    interest_rate NUMERIC(6, 3) NOT NULL,
    processing_fee_percent NUMERIC(6, 3) NOT NULL,
    required_documents VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE SEQUENCE application_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE loan_applications (
    id UUID PRIMARY KEY,
    application_number VARCHAR(32) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES users (id),
    loan_product_id UUID NOT NULL REFERENCES loan_products (id),
    requested_amount NUMERIC(15, 2) NOT NULL,
    tenure_months INTEGER NOT NULL,
    interest_rate NUMERIC(6, 3) NOT NULL,
    processing_fee_percent NUMERIC(6, 3) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    full_name VARCHAR(160),
    date_of_birth DATE,
    gender VARCHAR(20),
    national_id VARCHAR(40),
    phone VARCHAR(20),
    email VARCHAR(180),
    address_line VARCHAR(255),
    city VARCHAR(80),
    state VARCHAR(80),
    postal_code VARCHAR(20),
    employment_type VARCHAR(30),
    employer_name VARCHAR(160),
    designation VARCHAR(120),
    years_employed INTEGER,
    monthly_income NUMERIC(15, 2),
    other_income NUMERIC(15, 2),
    existing_emis NUMERIC(15, 2),
    monthly_expenses NUMERIC(15, 2),
    assigned_maker_id UUID REFERENCES users (id),
    assigned_checker_id UUID REFERENCES users (id),
    submitted_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_applications_customer ON loan_applications (customer_id);
CREATE INDEX idx_applications_status ON loan_applications (status);
CREATE INDEX idx_applications_maker ON loan_applications (assigned_maker_id);
CREATE INDEX idx_applications_checker ON loan_applications (assigned_checker_id);
CREATE INDEX idx_applications_created ON loan_applications (created_at DESC);

CREATE TABLE loan_documents (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    document_type VARCHAR(40) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users (id),
    verification_status VARCHAR(20) NOT NULL,
    verified_by UUID REFERENCES users (id),
    verified_at TIMESTAMPTZ,
    verification_remarks VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_application ON loan_documents (application_id);

CREATE TABLE maker_reviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    maker_id UUID NOT NULL REFERENCES users (id),
    customer_info_verified BOOLEAN NOT NULL DEFAULT FALSE,
    documents_verified BOOLEAN NOT NULL DEFAULT FALSE,
    financials_verified BOOLEAN NOT NULL DEFAULT FALSE,
    remarks TEXT,
    missing_information TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_maker_reviews_application ON maker_reviews (application_id);

CREATE TABLE checker_reviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    checker_id UUID NOT NULL REFERENCES users (id),
    decision VARCHAR(20) NOT NULL,
    reason TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_checker_reviews_application ON checker_reviews (application_id);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    changed_by UUID NOT NULL REFERENCES users (id),
    remarks VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_status_history_application ON application_status_history (application_id, created_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users (id),
    user_email VARCHAR(180),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    application_id UUID REFERENCES loan_applications (id),
    old_status VARCHAR(32),
    new_status VARCHAR(32),
    remarks TEXT,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_application ON audit_logs (application_id);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    application_id UUID REFERENCES loan_applications (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notifications_user ON notifications (user_id, read, created_at DESC);
