-- V3__Add_owner_role_for_group_members.sql
-- Extends group member roles with OWNER and maps existing creators to OWNER.

ALTER TABLE group_members
DROP CONSTRAINT IF EXISTS chk_role;

UPDATE group_members gm
SET role = 'OWNER'
FROM groups g
WHERE gm.group_id = g.id
  AND gm.user_id = g.created_by
  AND gm.role <> 'OWNER';

ALTER TABLE group_members
ADD CONSTRAINT chk_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'));
