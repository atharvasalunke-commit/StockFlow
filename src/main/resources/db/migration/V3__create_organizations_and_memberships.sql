CREATE TABLE organizations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_by BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT fk_organizations_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE memberships (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    organization_id BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    role            VARCHAR(20) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    CONSTRAINT pk_memberships PRIMARY KEY (id),
    -- a user can be in an organization only once
    CONSTRAINT uk_memberships_org_user UNIQUE (organization_id, user_id),
    CONSTRAINT fk_memberships_org FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'STAFF'))
) ENGINE = InnoDB;

-- "which organizations does this user belong to?"
CREATE INDEX idx_memberships_user ON memberships (user_id);