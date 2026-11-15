package com.stockflow.service;

import com.stockflow.common.BadRequestException;
import com.stockflow.common.ConflictException;
import com.stockflow.common.ResourceNotFoundException;
import com.stockflow.dto.AddMemberRequest;
import com.stockflow.dto.MemberResponse;
import com.stockflow.dto.OrganizationRequest;
import com.stockflow.dto.OrganizationResponse;
import com.stockflow.entity.Membership;
import com.stockflow.entity.Organization;
import com.stockflow.entity.Role;
import com.stockflow.entity.User;
import com.stockflow.repository.MembershipRepository;
import com.stockflow.repository.OrganizationRepository;
import com.stockflow.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationAccessService access;

    public OrganizationService(OrganizationRepository organizationRepository,
                               MembershipRepository membershipRepository,
                               UserRepository userRepository,
                               OrganizationAccessService access) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.access = access;
    }
    // org and owner membership go in one transaction, so we never get an org without an owner
    @Transactional
    public OrganizationResponse create(Long userId, OrganizationRequest request) {
        Organization org = organizationRepository.saveAndFlush(new Organization(request.name().trim(), userId));
        membershipRepository.save(new Membership(org.getId(), userId, Role.OWNER));
        return OrganizationResponse.of(org, Role.OWNER);
    }
    @Transactional(readOnly = true)
    public List<OrganizationResponse> listMine(Long userId) {
        Map<Long, Role> roleByOrg = new HashMap<>();
        for (Membership m : membershipRepository.findByUserId(userId)) {
            roleByOrg.put(m.getOrganizationId(), m.getRole());
        }
        List<OrganizationResponse> result = new ArrayList<>();
        for (Organization org : organizationRepository.findAllById(roleByOrg.keySet())) {
            result.add(OrganizationResponse.of(org, roleByOrg.get(org.getId())));
        }
        result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return result;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(Long organizationId, Long userId) {
        Membership membership = access.requireMember(organizationId, userId);
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return OrganizationResponse.of(org, membership.getRole());
    }

    @Transactional
    public OrganizationResponse update(Long organizationId, Long userId, OrganizationRequest request) {
        Membership membership = access.requireRole(organizationId, userId, Role.OWNER);
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        org.rename(request.name().trim());
        return OrganizationResponse.of(org, membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long organizationId, Long userId) {
        access.requireMember(organizationId, userId);
        List<Membership> memberships = membershipRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
        List<Long> userIds = new ArrayList<>();
        for (Membership m : memberships) {
            userIds.add(m.getUserId());
        }
        Map<Long, User> users = new HashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            users.put(u.getId(), u);
        }
        List<MemberResponse> result = new ArrayList<>();
        for (Membership m : memberships) {
            result.add(MemberResponse.of(users.get(m.getUserId()), m));
        }
        return result;
    }

    @Transactional
    public MemberResponse addMember(Long organizationId, Long userId, AddMemberRequest request) {
        access.requireRole(organizationId, userId, Role.OWNER);
        if (request.role() == Role.OWNER) {
            throw new BadRequestException("Role must be ADMIN or STAFF");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User target = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("No user with this email"));
        if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, target.getId())) {
            throw new ConflictException("User is already a member of this organization");
        }
        try {
            // the exists check above can race; the UNIQUE constraint is what really stops duplicates
            Membership saved = membershipRepository.saveAndFlush(
                    new Membership(organizationId, target.getId(), request.role()));
            return MemberResponse.of(target, saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("User is already a member of this organization");
        }
    }
}