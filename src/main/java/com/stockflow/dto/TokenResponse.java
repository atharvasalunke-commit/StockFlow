package com.stockflow.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
