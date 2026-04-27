-- V2__Add_group_visibility.sql
-- Adds visibility controls for public/private group access.

ALTER TABLE groups
ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

ALTER TABLE groups
ADD CONSTRAINT chk_group_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
