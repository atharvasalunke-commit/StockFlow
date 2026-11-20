package com.stockflow.dto;

import com.stockflow.entity.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
