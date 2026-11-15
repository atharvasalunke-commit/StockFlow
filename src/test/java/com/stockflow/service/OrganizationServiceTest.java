package com.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockflow.common.BadRequestException;
import com.stockflow.dto.AddMemberRequest;
import com.stockflow.dto.OrganizationRequest;
import com.stockflow.dto.OrganizationResponse;
import com.stockflow.entity.Membership;
import com.stockflow.entity.Organization;
import com.stockflow.entity.Role;
import com.stockflow.repository.MembershipRepository;
import com.stockflow.repository.OrganizationRepository;
import com.stockflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock MembershipRepository membershipRepository;
    @Mock UserRepository userRepository;
    @Mock OrganizationAccessService access;
    @InjectMocks OrganizationService service;

    @Test
    void create_makesCreatorTheOwner() {
        when(organizationRepository.saveAndFlush(any(Organization.class)))
                .thenAnswer(i -> i.getArgument(0));

        OrganizationResponse response = service.create(5L, new OrganizationRequest("  TechZone "));

        ArgumentCaptor<Membership> saved = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
        assertThat(saved.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(response.name()).isEqualTo("TechZone");
        assertThat(response.myRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void addMember_ownerRole_isRejected() {
        assertThatThrownBy(() -> service.addMember(1L, 5L, new AddMemberRequest("x@y.com", Role.OWNER)))
                .isInstanceOf(BadRequestException.class);
    }
}