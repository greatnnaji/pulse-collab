-- V6__Drop_group_visibility.sql
-- Removes deprecated group visibility once groups are strictly membership-scoped.

ALTER TABLE groups
DROP CONSTRAINT IF EXISTS chk_group_visibility;

ALTER TABLE groups
DROP COLUMN IF EXISTS visibility;
