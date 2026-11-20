-- owner_org_id holds the organization id only for OWNER rows and NULL for everyone else.
-- A UNIQUE index allows many NULLs, so this allows many ADMIN/STAFF rows but only one OWNER per organization.
ALTER TABLE memberships
    ADD COLUMN owner_org_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN role = 'OWNER' THEN organization_id END) STORED,
    ADD CONSTRAINT uk_memberships_one_owner UNIQUE (owner_org_id);
