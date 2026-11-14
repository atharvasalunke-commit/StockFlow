package com.stockflow.dto;

import com.stockflow.entity.User;

import java.time.Instant;

/** Deliberately has no passwordHash field. */
public record UserResponse(Long id, String email, String fullName, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
