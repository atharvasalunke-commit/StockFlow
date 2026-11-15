package com.stockflow.dto;

import com.stockflow.entity.Organization;
import com.stockflow.entity.Role;
import java.time.Instant;

public record OrganizationResponse(Long id, String name, Role myRole, Instant createdAt) {

    public static OrganizationResponse of(Organization org, Role myRole) {
        return new OrganizationResponse(org.getId(), org.getName(), myRole, org.getCreatedAt());
    }
}