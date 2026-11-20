package com.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockflow.common.BadRequestException;
import com.stockflow.common.ConflictException;
import com.stockflow.common.ResourceNotFoundException;
import com.stockflow.dto.AddMemberRequest;
import com.stockflow.dto.ChangeRoleRequest;
import com.stockflow.dto.OrganizationRequest;
import com.stockflow.dto.OrganizationResponse;
import com.stockflow.entity.Membership;
import com.stockflow.entity.Organization;
import com.stockflow.entity.Role;
import com.stockflow.entity.User;
import com.stockflow.repository.MembershipRepository;
import com.stockflow.repository.OrganizationRepository;
import com.stockflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock OrganizationAccessService access;

    @InjectMocks OrganizationService service;

    @Test
    void create_makesTheCreatorTheOwner() {
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(call -> call.getArgument(0));

        OrganizationResponse response = service.create(5L, new OrganizationRequest("  TechZone "));

        ArgumentCaptor<Membership> savedMembership = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(savedMembership.capture());

        assertThat(savedMembership.getValue().getUserId()).isEqualTo(5L);
        assertThat(savedMembership.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(response.name()).isEqualTo("TechZone");
        assertThat(response.myRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void addMember_withRoleOwner_isRejected() {
        AddMemberRequest request = new AddMemberRequest("x@y.com", Role.OWNER);

        assertThatThrownBy(() -> service.addMember(1L, 5L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addMember_unknownEmail_throwsNotFound() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        AddMemberRequest request = new AddMemberRequest("x@y.com", Role.STAFF);

        assertThatThrownBy(() -> service.addMember(1L, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMember_alreadyAMember_throwsConflict() {
        User existingUser = new User("x@y.com", "hash", "X");
        ReflectionTestUtils.setField(existingUser, "id", 9L);
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.of(existingUser));
        when(membershipRepository.existsByOrganizationIdAndUserId(1L, 9L)).thenReturn(true);
        AddMemberRequest request = new AddMemberRequest("x@y.com", Role.STAFF);

        assertThatThrownBy(() -> service.addMember(1L, 5L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeRole_toOwner_isRejected() {
        ChangeRoleRequest request = new ChangeRoleRequest(Role.OWNER);

        assertThatThrownBy(() -> service.changeRole(1L, 5L, 9L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void changeRole_ofSomeoneWhoIsNotAMember_throwsNotFound() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 9L)).thenReturn(Optional.empty());
        ChangeRoleRequest request = new ChangeRoleRequest(Role.ADMIN);

        assertThatThrownBy(() -> service.changeRole(1L, 5L, 9L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeRole_ofTheOwner_throwsConflict() {
        Membership ownerMembership = new Membership(1L, 9L, Role.OWNER);
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 9L)).thenReturn(Optional.of(ownerMembership));
        ChangeRoleRequest request = new ChangeRoleRequest(Role.STAFF);

        assertThatThrownBy(() -> service.changeRole(1L, 5L, 9L, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void removeMember_ofSomeoneWhoIsNotAMember_throwsNotFound() {
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(1L, 5L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeMember_theOwner_throwsConflict_andDeletesNothing() {
        Membership ownerMembership = new Membership(1L, 9L, Role.OWNER);
        when(membershipRepository.findByOrganizationIdAndUserId(1L, 9L)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> service.removeMember(1L, 5L, 9L))
                .isInstanceOf(ConflictException.class);
        verify(membershipRepository, never()).delete(any(Membership.class));
    }
}
