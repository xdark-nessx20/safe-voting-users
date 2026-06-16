package com.safevoting.users.application.auth;

public record AuthResult(String token, String email, String rol) {}
