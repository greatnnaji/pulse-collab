-- V7__Create_group_invites.sql
-- Group invites: owner/admin invites a user by email, invitee must accept before becoming a member.

CREATE TABLE group_invites (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    invited_user_id BIGINT NOT NULL,
    invited_by BIGINT NOT NULL,
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_invites_group FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_invites_invited_user FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_invites_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_invited_user UNIQUE (group_id, invited_user_id)
);

CREATE INDEX idx_group_invites_group_id ON group_invites(group_id);
CREATE INDEX idx_group_invites_invited_user_id ON group_invites(invited_user_id);
