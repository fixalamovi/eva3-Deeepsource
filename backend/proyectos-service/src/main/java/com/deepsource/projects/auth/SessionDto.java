package com.deepsource.projects.auth;
public record SessionDto(String uid, String name, String email, String role, boolean demo,
 String provider, String token, String refreshToken, long expiresAt) {}
