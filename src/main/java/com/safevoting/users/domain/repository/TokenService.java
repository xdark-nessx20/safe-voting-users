package com.safevoting.users.domain.repository;

public interface TokenService {

    String generarToken(String email, String rol);
}
