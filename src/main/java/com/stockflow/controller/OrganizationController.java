package com.stockflow.controller;

import com.stockflow.common.CurrentUser;
import com.stockflow.dto.AddMemberRequest;
import com.stockflow.dto.MemberResponse;
import com.stockflow.dto.OrganizationRequest;
import com.stockflow.dto.OrganizationResponse;
import com.stockflow.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody OrganizationRequest request) {
        return organizationService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping
    public List<OrganizationResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return organizationService.listMine(CurrentUser.id(jwt));
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long organizationId) {
        return organizationService.get(organizationId, CurrentUser.id(jwt));
    }

    @PutMapping("/{organizationId}")
    public OrganizationResponse update(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long organizationId,
                                       @Valid @RequestBody OrganizationRequest request) {
        return organizationService.update(organizationId, CurrentUser.id(jwt), request);
    }

    @GetMapping("/{organizationId}/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable Long organizationId) {
        return organizationService.listMembers(organizationId, CurrentUser.id(jwt));
    }

    @PostMapping("/{organizationId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long organizationId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return organizationService.addMember(organizationId, CurrentUser.id(jwt), request);
    }
}