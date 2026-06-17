package com.safevoting.users.domain.repository;

import java.util.UUID;

public interface TokenService {

    String generarToken(UUID usuarioId, String email, String rol);
}
