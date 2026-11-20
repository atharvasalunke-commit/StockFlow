package com.stockflow.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockflow.entity.Membership;
import com.stockflow.entity.Role;
import com.stockflow.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class OrganizationSecurityTest extends AbstractIntegrationTest {

    @Autowired
    MembershipRepository membershipRepository;
    TestUser owner;
    TestUser staff;
    TestUser outsider;
    long orgId;
    String orgUrl;
    String renameBody = """
            {"name": "New Name"}
            """;
    String makeAdminBody = """
            {"role": "ADMIN"}
            """;
    String makeStaffBody = """
            {"role": "STAFF"}
            """;
    String addMemberBody = """
            {"email": "someone@example.com", "role": "STAFF"}
            """;
    @BeforeEach
    void setUp() throws Exception {
        owner = newUser();
        staff = newUser();
        outsider = newUser();
        orgId = createOrganization(owner, "Acme Traders");
        orgUrl = "/api/organizations/" + orgId;
        addMember(owner, orgId, staff, "STAFF");
    }
    @Test
    void requestWithoutTokenGets401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(orgUrl))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(MockMvcRequestBuilders.put(orgUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void outsiderGets404OnEveryEndpoint() throws Exception {
        String memberUrl = orgUrl + "/members/" + staff.id();
        get(orgUrl, outsider).andExpect(status().isNotFound());
        put(orgUrl, outsider, renameBody).andExpect(status().isNotFound());
        get(orgUrl + "/members", outsider).andExpect(status().isNotFound());
        post(orgUrl + "/members", outsider, addMemberBody).andExpect(status().isNotFound());
        patch(memberUrl, outsider, makeAdminBody).andExpect(status().isNotFound());
        delete(memberUrl, outsider).andExpect(status().isNotFound());
    }

    @Test
    void outsiderDoesNotSeeTheOrganizationInHisList() throws Exception {
        get("/api/organizations", outsider)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void staffCanReadTheOrganizationAndItsMembers() throws Exception {
        get(orgUrl, staff).andExpect(status().isOk());
        get(orgUrl + "/members", staff).andExpect(status().isOk());
    }

    @Test
    void staffGets403WhenManagingTheOrganization() throws Exception {
        String memberUrl = orgUrl + "/members/" + staff.id();
        put(orgUrl, staff, renameBody).andExpect(status().isForbidden());
        post(orgUrl + "/members", staff, addMemberBody).andExpect(status().isForbidden());
        patch(memberUrl, staff, makeAdminBody).andExpect(status().isForbidden());
        delete(memberUrl, staff).andExpect(status().isForbidden());
    }

    @Test
    void ownerCanChangeAMembersRole() throws Exception {
        patch(orgUrl + "/members/" + staff.id(), owner, makeAdminBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        get("/api/organizations", staff)
                .andExpect(jsonPath("$[0].myRole").value("ADMIN"));
    }

    @Test
    void removedMemberLosesAccess() throws Exception {
        get(orgUrl, staff).andExpect(status().isOk());
        delete(orgUrl + "/members/" + staff.id(), owner).andExpect(status().isNoContent());
        get(orgUrl, staff).andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotBeDemotedOrRemoved() throws Exception {
        String ownerUrl = orgUrl + "/members/" + owner.id();
        patch(ownerUrl, owner, makeStaffBody).andExpect(status().isConflict());
        delete(ownerUrl, owner).andExpect(status().isConflict());
    }

    @Test
    void ownerCannotChangeOrRemoveAUserWhoBelongsToAnotherOrganization() throws Exception {
        createOrganization(outsider, "Other Shop");
        String url = orgUrl + "/members/" + outsider.id();
        patch(url, owner, makeAdminBody).andExpect(status().isNotFound());
        delete(url, owner).andExpect(status().isNotFound());
    }

    @Test
    void twoOrganizationsCanHaveTheSameName() throws Exception {
        String body = """
                {"name": "Acme Traders"}
                """;
        post("/api/organizations", owner, body)
                .andExpect(status().isCreated());
    }

    @Test
    void databaseRejectsASecondOwner() {
        Membership secondOwner = new Membership(orgId, outsider.id(), Role.OWNER);
        assertThatThrownBy(() -> membershipRepository.saveAndFlush(secondOwner))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_memberships_one_owner");
    }

    @Test
    void databaseRejectsTheSameUserTwiceInOneOrganization() {
        Membership duplicate = new Membership(orgId, staff.id(), Role.STAFF);
        assertThatThrownBy(() -> membershipRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_memberships_org_user");
    }
}
