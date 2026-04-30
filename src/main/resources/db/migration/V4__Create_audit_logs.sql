-- V4__Create_audit_logs.sql
-- Adds a lightweight audit trail for key auth and group actions.

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT NULL REFERENCES users (id),
    actor_identifier VARCHAR(255),
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    target_name VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_event_type ON audit_logs (event_type);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);