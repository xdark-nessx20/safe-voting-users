package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import java.util.List;

public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {}
