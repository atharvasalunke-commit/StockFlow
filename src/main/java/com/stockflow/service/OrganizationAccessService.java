package com.stockflow.service;

import com.stockflow.common.ForbiddenException;
import com.stockflow.common.ResourceNotFoundException;
import com.stockflow.entity.Membership;
import com.stockflow.entity.Role;
import com.stockflow.repository.MembershipRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAccessService {
    private final MembershipRepository membershipRepository;

    public OrganizationAccessService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }
    @Transactional(readOnly = true)
    public Membership requireMember(Long organizationId, Long userId) {
        return membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }
    @Transactional(readOnly = true)
    public Membership requireRole(Long organizationId, Long userId, Role... allowedRoles) {
        Membership membership = requireMember(organizationId, userId);
        if (!Set.of(allowedRoles).contains(membership.getRole())) {
            throw new ForbiddenException("You do not have permission to do this");
        }
        return membership;
    }
}
