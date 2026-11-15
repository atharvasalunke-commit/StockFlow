package com.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.stockflow.common.ForbiddenException;
import com.stockflow.common.ResourceNotFoundException;
import com.stockflow.entity.Membership;
import com.stockflow.entity.Role;
import com.stockflow.repository.MembershipRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTest {

    @Mock MembershipRepository membershipRepository;
    @InjectMocks OrganizationAccessService access;

    @Test
    void requireMember_nonMember_throwsNotFound() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireMember(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireRole_staffTriesOwnerAction_throwsForbidden() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(new Membership(1L, 2L, Role.STAFF)));
        assertThatThrownBy(() -> access.requireRole(1L, 2L, Role.OWNER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireRole_owner_isAllowed() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(new Membership(1L, 2L, Role.OWNER)));
        assertThat(access.requireRole(1L, 2L, Role.OWNER, Role.ADMIN).getRole()).isEqualTo(Role.OWNER);
    }
}