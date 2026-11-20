package com.stockflow.dto;

import com.stockflow.entity.Membership;
import com.stockflow.entity.Role;
import com.stockflow.entity.User;
import java.time.Instant;

public record MemberResponse(Long userId, String email, String fullName, Role role, Instant joinedAt) {

    public static MemberResponse of(User user, Membership membership) {
        return new MemberResponse(user.getId(), user.getEmail(), user.getFullName(),
                membership.getRole(), membership.getCreatedAt());
    }
}
